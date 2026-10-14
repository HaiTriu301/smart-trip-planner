package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.util.SecureTokens;
import com.trieu.tripplanner.dto.request.AcceptInvitationRequest;
import com.trieu.tripplanner.dto.request.ChangeMemberRoleRequest;
import com.trieu.tripplanner.dto.request.CreateShareLinkRequest;
import com.trieu.tripplanner.dto.request.InviteMemberRequest;
import com.trieu.tripplanner.dto.response.MemberResponse;
import com.trieu.tripplanner.dto.response.ShareLinkResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.ForbiddenException;
import com.trieu.tripplanner.exception.InvalidTokenException;
import com.trieu.tripplanner.exception.MemberAlreadyExistsException;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.MemberMapper;
import com.trieu.tripplanner.mapper.ShareLinkMapper;
import com.trieu.tripplanner.model.ShareLink;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripMember;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.model.enums.MemberStatus;
import com.trieu.tripplanner.model.enums.TripVisibility;
import com.trieu.tripplanner.repository.ShareLinkRepository;
import com.trieu.tripplanner.repository.TripMemberRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * <p>
 * Share links on share_links (rule 14.26): owner-only too. Their token is stored, but never logged either.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SharingServiceImpl implements SharingService {

    /** design.md rule 14.23: an invitation link works for 7 days. */
    static final Duration INVITE_TTL = Duration.ofDays(7);

    private static final String TRIP = "Trip";
    private static final String USER = "User";
    private static final String TRIP_MEMBER = "TripMember";
    private static final String SHARE_LINK = "ShareLink";

    /** design.md 5.2 "share_links": the token column is CHAR(32). */
    static final int LINK_TOKEN_LENGTH = 32;

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final UserRepository userRepository;
    private final MemberMapper memberMapper;
    private final ShareLinkMapper shareLinkMapper;
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

    @Override
    @Transactional
    public MemberResponse changeRole(Long tripId, Long memberId, ChangeMemberRoleRequest request) {
        TripMember member = findActiveMember(tripId, memberId);
        if (member.getRole() == request.role()) {
            // Nothing to write: Hibernate would find no dirty field anyway, and the log stays quiet
            return memberMapper.toResponse(member);
        }
        MemberRole previous = member.getRole();
        member.setRole(request.role());
        TripMember saved = tripMemberRepository.save(member);
        log.info("Trip {}: member {} role {} -> {}", tripId, memberId, previous, request.role());
        return memberMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void remove(Long tripId, Long memberId) {
        TripMember member = findActiveMember(tripId, memberId);
        MemberStatus previous = member.getStatus();
        member.setStatus(MemberStatus.REMOVED);
        // A withdrawn invitation must not be acceptable any more: nothing left to look the link up by
        member.setInviteTokenHash(null);
        member.setInviteExpiresAt(null);
        tripMemberRepository.save(member);
        log.info("Trip {}: member {} removed (was {} {})", tripId, memberId, previous, member.getRole());
    }

    @Override
    @Transactional
    public ShareLinkResponse createLink(Long tripId, CreateShareLinkRequest request) {
        Trip trip = findTrip(tripId);
        // One reading of the clock for the whole call: the expiry is checked and reported against the same instant
        Instant now = clock.instant();
        Instant expiresAt = linkExpiry(tripId, request, now);
        ShareLink link = shareLinkRepository.save(ShareLink.builder()
                .trip(trip)
                .token(unusedLinkToken())
                .expiresAt(expiresAt)
                // Only the owner reaches this method (controller guard), so the owner is the creator
                .createdBy(trip.getOwner())
                .build());
        // The trip is managed: the new label is written when the transaction commits. Nothing is written, and
        // the version stays, when the trip already has a link
        trip.setVisibility(TripVisibility.LINK);
        // The token opens the trip to whoever reads it: the log names the link by its id only (CLAUDE.md rule 17)
        log.info("Trip {}: share link {} created, expires {}", tripId, link.getId(),
                expiresAt != null ? expiresAt : "never");
        return shareLinkMapper.toResponse(link, now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShareLinkResponse> listLinks(Long tripId) {
        // Only here for the 404 of a missing or deleted trip: the links are found by the trip id alone
        findTrip(tripId);
        return shareLinkMapper.toResponses(
                shareLinkRepository.findByTripIdAndRevokedAtIsNullOrderByCreatedAtDescIdDesc(tripId),
                clock.instant());
    }

    @Override
    @Transactional
    public void revokeLink(Long tripId, Long linkId) {
        Trip trip = findTrip(tripId);
        // A link of another trip and a link revoked before both answer like a link that does not exist
        ShareLink link = shareLinkRepository.findByIdAndTripId(linkId, tripId)
                .filter(found -> found.getRevokedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException(SHARE_LINK, linkId));
        link.revoke(clock.instant());
        shareLinkRepository.save(link);

        // The label follows the links (rule 14.26): the last one gone, the trip is private again. The trip is
        // managed, so the label is written at commit; with another link left, nothing of the trip is written
        boolean lastLink = !shareLinkRepository.existsByTripIdAndRevokedAtIsNullAndIdNot(tripId, linkId);
        if (lastLink) {
            trip.setVisibility(TripVisibility.PRIVATE);
        }
        log.info("Trip {}: share link {} revoked{}", tripId, linkId, lastLink ? ", trip is private again" : "");
    }

    /**
     * A row of this trip that is still part of it (ACCEPTED or PENDING). The trip itself is checked first so a
     * deleted trip answers 404 before anything is read from trip_members (CLAUDE.md rule 15).
     */
    private TripMember findActiveMember(Long tripId, Long memberId) {
        findTrip(tripId);
        return tripMemberRepository.findByIdAndTripId(memberId, tripId)
                .filter(member -> member.getStatus() != MemberStatus.REMOVED)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP_MEMBER, memberId));
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

    /**
     * The expiry the owner asked for, or null for a link that never expires (no body, or no expiresAt). "Now"
     * comes from the Clock bean, never from the system clock (CLAUDE.md section 8). Cut to microseconds: MySQL
     * DATETIME(6) would round the nanoseconds, and the answer of this call must equal what a later read returns.
     */
    private Instant linkExpiry(Long tripId, CreateShareLinkRequest request, Instant now) {
        if (request == null || request.expiresAt() == null) {
            return null;
        }
        Instant expiresAt = request.expiresAt().truncatedTo(ChronoUnit.MICROS);
        if (!expiresAt.isAfter(now)) {
            throw BusinessRuleException.invalidField("expiresAt", "error.share-link.expires-not-future",
                    "Trip %d: share link expiry %s is not in the future".formatted(tripId, expiresAt));
        }
        return expiresAt;
    }

    /**
     * A token no link uses yet. 36^32 possible values make a collision a theoretical event, but the UNIQUE key
     * would turn one into a 500, so the token is checked and drawn again instead.
     */
    private String unusedLinkToken() {
        String token;
        do {
            token = SecureTokens.alphanumeric(LINK_TOKEN_LENGTH);
        } while (shareLinkRepository.existsByToken(token));
        return token;
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
