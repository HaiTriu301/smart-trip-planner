package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.util.SecureTokens;
import com.trieu.tripplanner.dto.request.AcceptInvitationRequest;
import com.trieu.tripplanner.dto.request.InviteMemberRequest;
import com.trieu.tripplanner.dto.response.MemberResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.ForbiddenException;
import com.trieu.tripplanner.exception.InvalidTokenException;
import com.trieu.tripplanner.exception.MemberAlreadyExistsException;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.MemberMapper;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripMember;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.model.enums.MemberStatus;
import com.trieu.tripplanner.repository.TripMemberRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Invitation lifecycle on trip_members. Only the owner reaches these methods (controller guard), so the owner is
 * also the inviter. The raw token exists only on its way to the mail: never stored, never logged (CLAUDE.md
 * rule 17).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SharingServiceImpl implements SharingService {

    /** design.md rule 14.23: an invitation link works for 7 days. */
    static final Duration INVITE_TTL = Duration.ofDays(7);

    private static final String TRIP = "Trip";
    private static final String USER = "User";

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final MemberMapper memberMapper;
    private final MailService mailService;
    private final Clock clock;

    @Override
    @Transactional
    public MemberResponse invite(Long tripId, InviteMemberRequest request) {
        Trip trip = findTrip(tripId);
        String email = normalizeEmail(request.email());
        User owner = trip.getOwner();
        if (email.equals(owner.getEmail())) {
            throw BusinessRuleException.invalidField("email", "error.member.owner-email",
                    "Trip %d: the owner invited their own email".formatted(tripId));
        }

        // One row per email on a trip (UNIQUE): an existing row is reopened, never duplicated
        TripMember member = tripMemberRepository.findByTripIdAndInvitedEmail(tripId, email)
                .map(existing -> reopen(existing, request.role()))
                .orElseGet(() -> newInvitation(trip, owner, email, request.role()));
        String rawToken = issueToken(member);
        TripMember saved = tripMemberRepository.save(member);

        // @Async: returns at once; a failed delivery only shows in the log (design.md 14.17)
        mailService.sendInvitationMail(email, owner.getFullName(), trip.getTitle(),
                request.role() == MemberRole.EDITOR, tripId, rawToken);
        // The email is personal data and the token is a secret: neither goes to the log
        log.info("Trip {}: invitation {} sent with role {}", tripId, saved.getId(), request.role());
        return memberMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MemberResponse accept(Long tripId, Long userId, AcceptInvitationRequest request) {
        // Only the hash is ever compared: the raw token never touches the database or the log
        TripMember member = tripMemberRepository.findByInviteTokenHash(SecureTokens.sha256Hex(request.token()))
                .orElseThrow(() -> new InvalidTokenException("unknown invitation token"));
        if (!member.getTrip().getId().equals(tripId)) {
            throw new InvalidTokenException("invitation %d is not for trip %d".formatted(member.getId(), tripId));
        }
        if (member.getStatus() != MemberStatus.PENDING) {
            throw new InvalidTokenException("invitation %d is %s".formatted(member.getId(), member.getStatus()));
        }
        Instant now = clock.instant();
        if (!member.getInviteExpiresAt().isAfter(now)) {
            throw new InvalidTokenException("invitation %d expired at %s".formatted(member.getId(),
                    member.getInviteExpiresAt()));
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER, userId));
        if (!user.getEmail().equalsIgnoreCase(member.getInvitedEmail())) {
            // A forwarded link: the invitation stays pending for the person it was sent to
            throw new ForbiddenException("invitation %d was sent to another email than user %d's"
                    .formatted(member.getId(), userId));
        }

        member.setUser(user);
        member.setStatus(MemberStatus.ACCEPTED);
        member.setAcceptedAt(now);
        // One-time link: nothing left to look up
        member.setInviteTokenHash(null);
        member.setInviteExpiresAt(null);
        TripMember saved = tripMemberRepository.save(member);
        log.info("Trip {}: invitation {} accepted by user {} as {}", tripId, saved.getId(), userId, saved.getRole());
        return memberMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long tripId) {
        Trip trip = findTrip(tripId);
        List<MemberResponse> members = new ArrayList<>();
        // trip.getOwner() loads the owner here (second query); the member rows come with their accounts (third)
        members.add(memberMapper.toOwnerResponse(trip.getOwner()));
        members.addAll(memberMapper.toResponses(tripMemberRepository.findActiveByTripId(tripId)));
        return members;
    }

    private TripMember newInvitation(Trip trip, User owner, String email, MemberRole role) {
        return TripMember.builder()
                .trip(trip)
                // Known account → linked now; unknown → linked when the person accepts after registering
                .user(userRepository.findByEmail(email).orElse(null))
                .invitedEmail(email)
                .role(role)
                .invitedBy(owner)
                .build();
    }

    /**
     * PENDING: the owner asked for the mail again (maybe with another role). REMOVED: the person is welcome back;
     * the account link survives the removal, so only the status and the role change. ACCEPTED: a conflict.
     */
    private TripMember reopen(TripMember existing, MemberRole role) {
        if (existing.getStatus() == MemberStatus.ACCEPTED) {
            throw new MemberAlreadyExistsException(existing.getTrip().getId(), existing.getId());
        }
        existing.setRole(role);
        existing.setStatus(MemberStatus.PENDING);
        existing.setAcceptedAt(null);
        if (existing.getUser() == null) {
            // The person may have registered since the first invitation
            userRepository.findByEmail(existing.getInvitedEmail()).ifPresent(existing::setUser);
        }
        return existing;
    }

    /** Stamps a fresh token (hash + 7-day expiry + invitedAt) on the row and returns the raw value for the mail. */
    private String issueToken(TripMember member) {
        String rawToken = SecureTokens.generate();
        Instant now = clock.instant();
        member.setInviteTokenHash(SecureTokens.sha256Hex(rawToken));
        member.setInviteExpiresAt(now.plus(INVITE_TTL));
        member.setInvitedAt(now);
        return rawToken;
    }

    private Trip findTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP, tripId));
    }

    /** Same normalisation as registration (User.normalizeEmail), so the UNIQUE key on invited_email holds. */
    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
