package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.util.SecureTokens;
import com.trieu.tripplanner.dto.request.InviteMemberRequest;
import com.trieu.tripplanner.dto.response.MemberResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.MemberMapper;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripMember;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.repository.TripMemberRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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

        String rawToken = SecureTokens.generate();
        Instant now = clock.instant();
        TripMember member = TripMember.builder()
                .trip(trip)
                // Known account → linked now; unknown → linked when the person accepts after registering
                .user(userRepository.findByEmail(email).orElse(null))
                .invitedEmail(email)
                .role(request.role())
                .inviteTokenHash(SecureTokens.sha256Hex(rawToken))
                .inviteExpiresAt(now.plus(INVITE_TTL))
                .invitedBy(owner)
                .invitedAt(now)
                .build();
        TripMember saved = tripMemberRepository.save(member);

        // @Async: returns at once; a failed delivery only shows in the log (design.md 14.17)
        mailService.sendInvitationMail(email, owner.getFullName(), trip.getTitle(),
                request.role() == MemberRole.EDITOR, tripId, rawToken);
        // The email is personal data and the token is a secret: neither goes to the log
        log.info("Trip {}: invitation {} created with role {}", tripId, saved.getId(), request.role());
        return memberMapper.toResponse(saved);
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
