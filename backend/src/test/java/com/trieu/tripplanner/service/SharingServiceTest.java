package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.common.util.SecureTokens;
import com.trieu.tripplanner.dto.request.AcceptInvitationRequest;
import com.trieu.tripplanner.dto.request.ChangeMemberRoleRequest;
import com.trieu.tripplanner.dto.request.CreateShareLinkRequest;
import com.trieu.tripplanner.dto.request.InviteMemberRequest;
import com.trieu.tripplanner.dto.response.MemberResponse;
import com.trieu.tripplanner.dto.response.ShareLinkResponse;
import com.trieu.tripplanner.dto.response.TripRole;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.FieldViolation;
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
import com.trieu.tripplanner.model.enums.SharePermission;
import com.trieu.tripplanner.model.enums.TripVisibility;
import com.trieu.tripplanner.repository.ShareLinkRepository;
import com.trieu.tripplanner.repository.TripMemberRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.support.TestUsers;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Repositories, the mail service and the clock are mocks; the MapStruct mapper is the real generated one.
 * Time stands still at {@link #NOW}, so the 7-day expiry can be asserted exactly.
 */
@ExtendWith(MockitoExtension.class)
class SharingServiceTest {

    private static final long TRIP_ID = 5L;
    private static final long OWNER_ID = 7L;
    private static final long MEMBER_ID = 31L;
    private static final long LINK_ID = 41L;
    private static final String LINK_TOKEN = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
    private static final String OTHER_LINK_TOKEN = "z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4";
    private static final Instant NOW = Instant.parse("2026-10-08T03:00:00Z");
    private static final String RAW_TOKEN = "raw-invitation-token-from-the-mail-link";

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private ShareLinkRepository shareLinkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    private SharingServiceImpl sharingService;
    private User owner;
    private Trip trip;

    @BeforeEach
    void setUp() {
        owner = TestUsers.verified(OWNER_ID, "owner@example.com");
        trip = Trip.builder()
                .owner(owner)
                .title("Đà Lạt mùa hoa")
                .slug("da-lat-mua-hoa-x7k2qp")
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 3))
                .build();
        ReflectionTestUtils.setField(trip, "id", TRIP_ID);
        sharingService = new SharingServiceImpl(tripRepository, tripMemberRepository, shareLinkRepository,
                userRepository, Mappers.getMapper(MemberMapper.class), Mappers.getMapper(ShareLinkMapper.class),
                mailService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void invitesANewEmailAsPendingWithAHashedTokenThatExpiresInSevenDays() {
        tripExists();
        noRowFor("friend@example.com");
        when(userRepository.findByEmail("friend@example.com")).thenReturn(Optional.empty());
        saveAssignsId();

        MemberResponse response = sharingService.invite(TRIP_ID,
                new InviteMemberRequest("friend@example.com", MemberRole.EDITOR));

        TripMember saved = savedMember();
        assertThat(saved.getTrip()).isSameAs(trip);
        assertThat(saved.getUser()).isNull();
        assertThat(saved.getInvitedEmail()).isEqualTo("friend@example.com");
        assertThat(saved.getRole()).isEqualTo(MemberRole.EDITOR);
        assertThat(saved.getStatus()).isEqualTo(MemberStatus.PENDING);
        assertThat(saved.getInvitedBy()).isSameAs(owner);
        assertThat(saved.getInvitedAt()).isEqualTo(NOW);
        assertThat(saved.getInviteExpiresAt()).isEqualTo(NOW.plus(7, ChronoUnit.DAYS));
        assertThat(saved.getAcceptedAt()).isNull();

        // The mail carries the raw token; the row holds only its SHA-256, so the two must differ and match
        String rawToken = mailedToken();
        assertThat(rawToken).hasSize(64).isNotEqualTo(saved.getInviteTokenHash());
        assertThat(saved.getInviteTokenHash()).isEqualTo(SecureTokens.sha256Hex(rawToken));
        verify(mailService).sendInvitationMail("friend@example.com", owner.getFullName(), "Đà Lạt mùa hoa",
                true, TRIP_ID, rawToken);

        assertThat(response).isEqualTo(new MemberResponse(MEMBER_ID, null, null, "friend@example.com", null,
                TripRole.EDITOR, MemberStatus.PENDING, NOW, null));
    }

    @Test
    void normalisesTheEmailBeforeStoringAndMailing() {
        tripExists();
        noRowFor("friend@example.com");
        when(userRepository.findByEmail("friend@example.com")).thenReturn(Optional.empty());
        saveAssignsId();

        sharingService.invite(TRIP_ID, new InviteMemberRequest("  Friend@Example.COM ", MemberRole.VIEWER));

        assertThat(savedMember().getInvitedEmail()).isEqualTo("friend@example.com");
        verify(tripMemberRepository).findByTripIdAndInvitedEmail(TRIP_ID, "friend@example.com");
        verify(mailService).sendInvitationMail(eq("friend@example.com"), anyString(), anyString(), anyBoolean(),
                anyLong(), anyString());
    }

    // ---- an email that already has a row on the trip ---------------------------------------------------------

    @Test
    void reinvitingAPendingEmailIssuesANewTokenAndMailsAgainOnTheSameRow() {
        tripExists();
        TripMember pending = existingRow("friend@example.com", MemberStatus.PENDING, null);
        String oldHash = pending.getInviteTokenHash();
        Instant oldInvitedAt = pending.getInvitedAt();
        when(userRepository.findByEmail("friend@example.com")).thenReturn(Optional.empty());
        saveReturnsArgument();

        MemberResponse response = sharingService.invite(TRIP_ID,
                new InviteMemberRequest("friend@example.com", MemberRole.EDITOR));

        // Same row, fresh token: the first link stops working, the mail carries the second one
        assertThat(savedMember()).isSameAs(pending);
        assertThat(pending.getStatus()).isEqualTo(MemberStatus.PENDING);
        assertThat(pending.getInviteTokenHash()).isNotEqualTo(oldHash).isEqualTo(SecureTokens.sha256Hex(mailedToken()));
        assertThat(pending.getInviteExpiresAt()).isEqualTo(NOW.plus(7, ChronoUnit.DAYS));
        assertThat(pending.getInvitedAt()).isEqualTo(NOW).isNotEqualTo(oldInvitedAt);
        assertThat(pending.getRole()).as("the role sent with the resend wins").isEqualTo(MemberRole.EDITOR);
        assertThat(response.memberId()).isEqualTo(MEMBER_ID);
        assertThat(response.role()).isEqualTo(TripRole.EDITOR);
    }

    @Test
    void reinvitingAPendingEmailLinksTheAccountCreatedSinceTheFirstMail() {
        tripExists();
        existingRow("friend@example.com", MemberStatus.PENDING, null);
        User friend = TestUsers.verified(9L, "friend@example.com");
        when(userRepository.findByEmail("friend@example.com")).thenReturn(Optional.of(friend));
        saveReturnsArgument();

        MemberResponse response = sharingService.invite(TRIP_ID,
                new InviteMemberRequest("friend@example.com", MemberRole.VIEWER));

        assertThat(savedMember().getUser()).isSameAs(friend);
        assertThat(response.userId()).isEqualTo(9L);
    }

    @Test
    void reinvitingARemovedMemberReopensTheRowAsPendingAndKeepsTheAccountLink() {
        tripExists();
        User friend = TestUsers.verified(9L, "friend@example.com");
        TripMember removed = existingRow("friend@example.com", MemberStatus.REMOVED, friend);
        removed.setAcceptedAt(Instant.parse("2026-09-01T00:00:00Z"));
        removed.setInviteTokenHash(null);
        removed.setInviteExpiresAt(null);
        saveReturnsArgument();

        MemberResponse response = sharingService.invite(TRIP_ID,
                new InviteMemberRequest("friend@example.com", MemberRole.VIEWER));

        assertThat(savedMember()).isSameAs(removed);
        assertThat(removed.getStatus()).isEqualTo(MemberStatus.PENDING);
        assertThat(removed.getAcceptedAt()).as("has to accept again").isNull();
        assertThat(removed.getUser()).isSameAs(friend);
        assertThat(removed.getInviteTokenHash()).isEqualTo(SecureTokens.sha256Hex(mailedToken()));
        assertThat(removed.getInviteExpiresAt()).isEqualTo(NOW.plus(7, ChronoUnit.DAYS));
        // The account is known already, so no lookup is needed
        verify(userRepository, never()).findByEmail(anyString());
        assertThat(response.status()).isEqualTo(MemberStatus.PENDING);
        assertThat(response.acceptedAt()).isNull();
    }

    @Test
    void reinvitingAnAcceptedMemberIs409AndChangesNothing() {
        tripExists();
        User friend = TestUsers.verified(9L, "friend@example.com");
        TripMember accepted = existingRow("friend@example.com", MemberStatus.ACCEPTED, friend);
        accepted.setInviteTokenHash(null);

        assertThatThrownBy(() -> sharingService.invite(TRIP_ID,
                new InviteMemberRequest("friend@example.com", MemberRole.EDITOR)))
                .isInstanceOf(MemberAlreadyExistsException.class)
                .satisfies(ex -> assertThat(((MemberAlreadyExistsException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEMBER_ALREADY_EXISTS));

        assertThat(accepted.getStatus()).isEqualTo(MemberStatus.ACCEPTED);
        assertThat(accepted.getRole()).isEqualTo(MemberRole.VIEWER);
        assertThat(accepted.getInviteTokenHash()).isNull();
        verify(tripMemberRepository, never()).save(any());
        verifyNoInteractions(mailService, userRepository);
    }

    @Test
    void linksTheAccountWhenTheEmailAlreadyBelongsToOne() {
        tripExists();
        noRowFor("friend@example.com");
        User friend = TestUsers.verified(9L, "friend@example.com");
        when(userRepository.findByEmail("friend@example.com")).thenReturn(Optional.of(friend));
        saveAssignsId();

        MemberResponse response = sharingService.invite(TRIP_ID,
                new InviteMemberRequest("friend@example.com", MemberRole.VIEWER));

        assertThat(savedMember().getUser()).isSameAs(friend);
        assertThat(savedMember().getStatus()).as("still has to accept").isEqualTo(MemberStatus.PENDING);
        assertThat(response.userId()).isEqualTo(9L);
        assertThat(response.fullName()).isEqualTo(friend.getFullName());
        assertThat(response.role()).isEqualTo(TripRole.VIEWER);
    }

    @Test
    void viewerInvitationTellsTheMailItIsViewOnly() {
        tripExists();
        noRowFor("friend@example.com");
        when(userRepository.findByEmail("friend@example.com")).thenReturn(Optional.empty());
        saveAssignsId();

        sharingService.invite(TRIP_ID, new InviteMemberRequest("friend@example.com", MemberRole.VIEWER));

        verify(mailService).sendInvitationMail(anyString(), anyString(), anyString(), eq(false), anyLong(),
                anyString());
    }

    @Test
    void rejectsTheOwnersOwnEmailWithoutSavingOrMailing() {
        tripExists();

        assertThatThrownBy(() -> sharingService.invite(TRIP_ID,
                new InviteMemberRequest(" Owner@Example.com", MemberRole.EDITOR)))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException rule = (BusinessRuleException) ex;
                    assertThat(rule.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(rule.getDetails()).containsExactly(
                            FieldViolation.of("email", "error.member.owner-email"));
                });
        verifyNoInteractions(tripMemberRepository, mailService, userRepository);
    }

    @Test
    void missingTripIs404BeforeAnythingElse() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.invite(TRIP_ID,
                new InviteMemberRequest("friend@example.com", MemberRole.VIEWER)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(tripMemberRepository, mailService, userRepository);
    }

    // ---- accept ------------------------------------------------------------------------------------------------

    @Test
    void acceptsAPendingInvitationForTheAccountItWasSentTo() {
        TripMember pending = pendingInvitation("friend@example.com", RAW_TOKEN);
        User friend = TestUsers.verified(9L, "friend@example.com");
        when(userRepository.findById(9L)).thenReturn(Optional.of(friend));
        saveReturnsArgument();

        MemberResponse response = sharingService.accept(TRIP_ID, 9L, new AcceptInvitationRequest(RAW_TOKEN));

        // Looked up by hash only; the raw token is never handed to the repository
        verify(tripMemberRepository).findByInviteTokenHash(SecureTokens.sha256Hex(RAW_TOKEN));
        verify(tripMemberRepository, never()).findByInviteTokenHash(RAW_TOKEN);
        assertThat(savedMember()).isSameAs(pending);
        assertThat(pending.getUser()).isSameAs(friend);
        assertThat(pending.getStatus()).isEqualTo(MemberStatus.ACCEPTED);
        assertThat(pending.getAcceptedAt()).isEqualTo(NOW);
        assertThat(pending.getInviteTokenHash()).as("one-time link").isNull();
        assertThat(pending.getInviteExpiresAt()).isNull();
        assertThat(pending.getRole()).isEqualTo(MemberRole.VIEWER);
        assertThat(response).isEqualTo(new MemberResponse(MEMBER_ID, 9L, friend.getFullName(),
                "friend@example.com", null, TripRole.VIEWER, MemberStatus.ACCEPTED, pending.getInvitedAt(), NOW));
        verifyNoInteractions(mailService);
    }

    @Test
    void acceptIgnoresTheCaseOfTheAccountEmail() {
        pendingInvitation("friend@example.com", RAW_TOKEN);
        when(userRepository.findById(9L)).thenReturn(Optional.of(TestUsers.verified(9L, "Friend@Example.com")));
        saveReturnsArgument();

        sharingService.accept(TRIP_ID, 9L, new AcceptInvitationRequest(RAW_TOKEN));

        assertThat(savedMember().getStatus()).isEqualTo(MemberStatus.ACCEPTED);
    }

    @Test
    void acceptWithAnUnknownTokenIs400() {
        when(tripMemberRepository.findByInviteTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.accept(TRIP_ID, 9L, new AcceptInvitationRequest("made-up")))
                .isInstanceOf(InvalidTokenException.class);
        verify(tripMemberRepository, never()).save(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void acceptWithATokenOfAnotherTripIs400() {
        pendingInvitation("friend@example.com", RAW_TOKEN);

        assertThatThrownBy(() -> sharingService.accept(TRIP_ID + 1, 9L, new AcceptInvitationRequest(RAW_TOKEN)))
                .isInstanceOf(InvalidTokenException.class);
        verify(tripMemberRepository, never()).save(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void acceptWithAnExpiredTokenIs400() {
        TripMember pending = pendingInvitation("friend@example.com", RAW_TOKEN);
        // Expiry is exclusive: a link whose deadline is exactly now is already dead
        pending.setInviteExpiresAt(NOW);

        assertThatThrownBy(() -> sharingService.accept(TRIP_ID, 9L, new AcceptInvitationRequest(RAW_TOKEN)))
                .isInstanceOf(InvalidTokenException.class);
        assertThat(pending.getStatus()).isEqualTo(MemberStatus.PENDING);
        verify(tripMemberRepository, never()).save(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void acceptOnARowThatIsNoLongerPendingIs400() {
        TripMember removed = pendingInvitation("friend@example.com", RAW_TOKEN);
        removed.setStatus(MemberStatus.REMOVED);

        assertThatThrownBy(() -> sharingService.accept(TRIP_ID, 9L, new AcceptInvitationRequest(RAW_TOKEN)))
                .isInstanceOf(InvalidTokenException.class);
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void acceptBySomeoneElseThanTheInvitedEmailIs403AndKeepsTheInvitationPending() {
        TripMember pending = pendingInvitation("friend@example.com", RAW_TOKEN);
        when(userRepository.findById(12L)).thenReturn(Optional.of(TestUsers.verified(12L, "stranger@example.com")));

        assertThatThrownBy(() -> sharingService.accept(TRIP_ID, 12L, new AcceptInvitationRequest(RAW_TOKEN)))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(ex -> assertThat(((ForbiddenException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        // The right person can still use the link
        assertThat(pending.getStatus()).isEqualTo(MemberStatus.PENDING);
        assertThat(pending.getUser()).isNull();
        assertThat(pending.getInviteTokenHash()).isEqualTo(SecureTokens.sha256Hex(RAW_TOKEN));
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void theSameLinkCannotBeUsedTwice() {
        pendingInvitation("friend@example.com", RAW_TOKEN);
        when(userRepository.findById(9L)).thenReturn(Optional.of(TestUsers.verified(9L, "friend@example.com")));
        saveReturnsArgument();
        sharingService.accept(TRIP_ID, 9L, new AcceptInvitationRequest(RAW_TOKEN));

        // The hash was cleared by the first call, so the database no longer finds the row by it
        when(tripMemberRepository.findByInviteTokenHash(SecureTokens.sha256Hex(RAW_TOKEN)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.accept(TRIP_ID, 9L, new AcceptInvitationRequest(RAW_TOKEN)))
                .isInstanceOf(InvalidTokenException.class);
    }

    // ---- listMembers -------------------------------------------------------------------------------------------

    @Test
    void listMembersPutsTheOwnerFirstThenTheRowsInRepositoryOrder() {
        tripExists();
        User friend = TestUsers.verified(9L, "friend@example.com");
        TripMember accepted = row("friend@example.com", MemberStatus.ACCEPTED, friend);
        accepted.setInviteTokenHash(null);
        accepted.setAcceptedAt(NOW.minus(1, ChronoUnit.DAYS));
        TripMember pending = row("waiting@example.com", MemberStatus.PENDING, null);
        ReflectionTestUtils.setField(pending, "id", MEMBER_ID + 1);
        when(tripMemberRepository.findActiveByTripId(TRIP_ID)).thenReturn(List.of(accepted, pending));

        List<MemberResponse> members = sharingService.listMembers(TRIP_ID);

        assertThat(members).containsExactly(
                new MemberResponse(null, OWNER_ID, owner.getFullName(), "owner@example.com", null, TripRole.OWNER,
                        MemberStatus.ACCEPTED, null, null),
                new MemberResponse(MEMBER_ID, 9L, friend.getFullName(), "friend@example.com", null, TripRole.VIEWER,
                        MemberStatus.ACCEPTED, accepted.getInvitedAt(), accepted.getAcceptedAt()),
                new MemberResponse(MEMBER_ID + 1, null, null, "waiting@example.com", null, TripRole.VIEWER,
                        MemberStatus.PENDING, pending.getInvitedAt(), null));
    }

    @Test
    void listMembersOfATripWithoutMembersIsJustTheOwner() {
        tripExists();
        when(tripMemberRepository.findActiveByTripId(TRIP_ID)).thenReturn(List.of());

        assertThat(sharingService.listMembers(TRIP_ID)).singleElement()
                .satisfies(member -> {
                    assertThat(member.role()).isEqualTo(TripRole.OWNER);
                    assertThat(member.memberId()).isNull();
                });
    }

    @Test
    void listMembersOfAMissingTripIs404() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.listMembers(TRIP_ID)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(tripMemberRepository);
    }

    // ---- changeRole --------------------------------------------------------------------------------------------

    @Test
    void changeRolePromotesAViewerToEditor() {
        tripExists();
        TripMember viewer = memberOfTrip(MemberStatus.ACCEPTED);
        saveReturnsArgument();

        MemberResponse response = sharingService.changeRole(TRIP_ID, MEMBER_ID,
                new ChangeMemberRoleRequest(MemberRole.EDITOR));

        assertThat(savedMember()).isSameAs(viewer);
        assertThat(viewer.getRole()).isEqualTo(MemberRole.EDITOR);
        assertThat(viewer.getStatus()).as("status untouched").isEqualTo(MemberStatus.ACCEPTED);
        assertThat(response.role()).isEqualTo(TripRole.EDITOR);
    }

    @Test
    void changeRoleAlsoWorksOnAPendingInvitation() {
        tripExists();
        TripMember pending = memberOfTrip(MemberStatus.PENDING);
        saveReturnsArgument();

        sharingService.changeRole(TRIP_ID, MEMBER_ID, new ChangeMemberRoleRequest(MemberRole.EDITOR));

        assertThat(pending.getRole()).isEqualTo(MemberRole.EDITOR);
        assertThat(pending.getStatus()).isEqualTo(MemberStatus.PENDING);
    }

    @Test
    void changeRoleToTheCurrentRoleAnswersWithoutWriting() {
        tripExists();
        memberOfTrip(MemberStatus.ACCEPTED);

        MemberResponse response = sharingService.changeRole(TRIP_ID, MEMBER_ID,
                new ChangeMemberRoleRequest(MemberRole.VIEWER));

        assertThat(response.role()).isEqualTo(TripRole.VIEWER);
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void changeRoleOfARemovedMemberIs404() {
        tripExists();
        TripMember removed = memberOfTrip(MemberStatus.REMOVED);

        assertThatThrownBy(() -> sharingService.changeRole(TRIP_ID, MEMBER_ID,
                new ChangeMemberRoleRequest(MemberRole.EDITOR)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(removed.getRole()).isEqualTo(MemberRole.VIEWER);
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void changeRoleOfAMemberIdThatIsNotOnThisTripIs404() {
        tripExists();
        when(tripMemberRepository.findByIdAndTripId(MEMBER_ID, TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.changeRole(TRIP_ID, MEMBER_ID,
                new ChangeMemberRoleRequest(MemberRole.EDITOR)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void changeRoleOnAMissingTripIs404BeforeTheMemberIsLookedUp() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.changeRole(TRIP_ID, MEMBER_ID,
                new ChangeMemberRoleRequest(MemberRole.EDITOR)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(tripMemberRepository);
    }

    // ---- remove ------------------------------------------------------------------------------------------------

    @Test
    void removeMarksAnAcceptedMemberRemovedAndKeepsTheAccountLinkAndRole() {
        tripExists();
        TripMember accepted = memberOfTrip(MemberStatus.ACCEPTED);
        accepted.setInviteTokenHash(null);
        accepted.setInviteExpiresAt(null);
        accepted.setAcceptedAt(NOW.minus(1, ChronoUnit.DAYS));
        saveReturnsArgument();

        sharingService.remove(TRIP_ID, MEMBER_ID);

        // The row is kept (design.md rule 14.25): re-inviting reuses it, the history stays readable
        assertThat(savedMember()).isSameAs(accepted);
        assertThat(accepted.getStatus()).isEqualTo(MemberStatus.REMOVED);
        assertThat(accepted.getUser()).isNotNull();
        assertThat(accepted.getRole()).isEqualTo(MemberRole.VIEWER);
        assertThat(accepted.getAcceptedAt()).isEqualTo(NOW.minus(1, ChronoUnit.DAYS));
        verifyNoInteractions(mailService, userRepository);
    }

    @Test
    void removeWithdrawsAPendingInvitationByClearingItsToken() {
        tripExists();
        TripMember pending = memberOfTrip(MemberStatus.PENDING);
        saveReturnsArgument();

        sharingService.remove(TRIP_ID, MEMBER_ID);

        assertThat(pending.getStatus()).isEqualTo(MemberStatus.REMOVED);
        // Without the hash the link in the mail no longer finds any row: it is dead, not just "not pending"
        assertThat(pending.getInviteTokenHash()).isNull();
        assertThat(pending.getInviteExpiresAt()).isNull();
    }

    @Test
    void removeOfAnAlreadyRemovedMemberIs404AndWritesNothing() {
        tripExists();
        memberOfTrip(MemberStatus.REMOVED);

        assertThatThrownBy(() -> sharingService.remove(TRIP_ID, MEMBER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void removeOfAMemberIdThatIsNotOnThisTripIs404() {
        tripExists();
        when(tripMemberRepository.findByIdAndTripId(MEMBER_ID, TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.remove(TRIP_ID, MEMBER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(tripMemberRepository, never()).save(any());
    }

    @Test
    void removeOnAMissingTripIs404BeforeTheMemberIsLookedUp() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.remove(TRIP_ID, MEMBER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(tripMemberRepository);
    }

    // ---- share links -------------------------------------------------------------------------------------------

    @Test
    void createsAViewLinkWithARandomTokenAndMarksTheTripAsSharedByLinkWhenTheCallHasNoBody() {
        tripExists();
        linkSaveAssignsId();
        assertThat(trip.getVisibility()).isEqualTo(TripVisibility.PRIVATE);

        ShareLinkResponse response = sharingService.createLink(TRIP_ID, null);

        ShareLink saved = savedLink();
        assertThat(saved.getTrip()).isSameAs(trip);
        assertThat(saved.getCreatedBy()).isSameAs(owner);
        assertThat(saved.getToken()).matches("[a-z0-9]{32}");
        assertThat(saved.getPermission()).isEqualTo(SharePermission.VIEW);
        assertThat(saved.getExpiresAt()).isNull();
        assertThat(saved.getRevokedAt()).isNull();
        assertThat(saved.getViewCount()).isZero();
        // The label of the trip follows its links (design.md rule 14.26)
        assertThat(trip.getVisibility()).isEqualTo(TripVisibility.LINK);

        assertThat(response.id()).isEqualTo(LINK_ID);
        assertThat(response.token()).isEqualTo(saved.getToken());
        assertThat(response.permission()).isEqualTo(SharePermission.VIEW);
        assertThat(response.expiresAt()).isNull();
        assertThat(response.viewCount()).isZero();
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.expired()).isFalse();
        // Nothing about members is touched, and nothing is mailed
        verifyNoInteractions(tripMemberRepository, mailService);
    }

    @Test
    void everyLinkOfATripGetsItsOwnToken() {
        tripExists();
        linkSaveAssignsId();

        String first = sharingService.createLink(TRIP_ID, null).token();
        String second = sharingService.createLink(TRIP_ID, null).token();

        assertThat(second).matches("[a-z0-9]{32}").isNotEqualTo(first);
        verify(shareLinkRepository, times(2)).save(any(ShareLink.class));
        assertThat(trip.getVisibility()).isEqualTo(TripVisibility.LINK);
    }

    @Test
    void drawsAnotherTokenWhenTheFirstOneIsAlreadyInUse() {
        tripExists();
        linkSaveAssignsId();
        when(shareLinkRepository.existsByToken(anyString())).thenReturn(true, false);

        sharingService.createLink(TRIP_ID, null);

        ArgumentCaptor<String> candidates = ArgumentCaptor.forClass(String.class);
        verify(shareLinkRepository, times(2)).existsByToken(candidates.capture());
        String taken = candidates.getAllValues().get(0);
        String free = candidates.getAllValues().get(1);
        assertThat(free).isNotEqualTo(taken);
        assertThat(savedLink().getToken()).isEqualTo(free);
    }

    @Test
    void createsALinkThatExpiresAtTheMomentTheOwnerAsked() {
        tripExists();
        linkSaveAssignsId();
        Instant inAWeek = NOW.plus(7, ChronoUnit.DAYS);

        ShareLinkResponse response = sharingService.createLink(TRIP_ID, new CreateShareLinkRequest(inAWeek));

        assertThat(savedLink().getExpiresAt()).isEqualTo(inAWeek);
        assertThat(response.expiresAt()).isEqualTo(inAWeek);
        assertThat(response.expired()).isFalse();
        assertThat(trip.getVisibility()).isEqualTo(TripVisibility.LINK);
    }

    @Test
    void aBodyWithoutAnExpiryMakesALinkThatNeverExpires() {
        tripExists();
        linkSaveAssignsId();

        ShareLinkResponse response = sharingService.createLink(TRIP_ID, new CreateShareLinkRequest(null));

        assertThat(savedLink().getExpiresAt()).isNull();
        assertThat(response.expiresAt()).isNull();
    }

    @Test
    void anExpiryOneMicrosecondAheadIsStillTheFuture() {
        tripExists();
        linkSaveAssignsId();
        Instant justAhead = NOW.plus(1, ChronoUnit.MICROS);

        sharingService.createLink(TRIP_ID, new CreateShareLinkRequest(justAhead));

        assertThat(savedLink().getExpiresAt()).isEqualTo(justAhead);
    }

    @Test
    void keepsTheExpiryToTheMicrosecondLikeTheColumn() {
        tripExists();
        linkSaveAssignsId();
        // DATETIME(6) would round the last three digits; the answer must equal what is read back later
        Instant withNanoseconds = NOW.plusSeconds(60).plusNanos(123_456_789);

        ShareLinkResponse response = sharingService.createLink(TRIP_ID, new CreateShareLinkRequest(withNanoseconds));

        Instant toTheMicrosecond = NOW.plusSeconds(60).plusNanos(123_456_000);
        assertThat(savedLink().getExpiresAt()).isEqualTo(toTheMicrosecond);
        assertThat(response.expiresAt()).isEqualTo(toTheMicrosecond);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("expiriesThatAreNotInTheFuture")
    void refusesAnExpiryThatIsNotInTheFutureAndStoresNothing(String label, Instant expiresAt) {
        tripExists();

        assertThatThrownBy(() -> sharingService.createLink(TRIP_ID, new CreateShareLinkRequest(expiresAt)))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException rule = (BusinessRuleException) ex;
                    assertThat(rule.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(rule.getDetails()).containsExactly(
                            FieldViolation.of("expiresAt", "error.share-link.expires-not-future"));
                });
        verifyNoInteractions(shareLinkRepository);
        // The trip is not marked as shared when no link was made
        assertThat(trip.getVisibility()).isEqualTo(TripVisibility.PRIVATE);
    }

    static Stream<Arguments> expiriesThatAreNotInTheFuture() {
        return Stream.of(
                Arguments.of("one second ago", NOW.minusSeconds(1)),
                Arguments.of("exactly now", NOW),
                // Ahead of now only by what the column cannot hold
                Arguments.of("999 nanoseconds ahead", NOW.plusNanos(999)));
    }

    @Test
    void createLinkOnAMissingTripIs404AndStoresNothing() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.createLink(TRIP_ID, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(shareLinkRepository);
    }

    @Test
    void listsTheLiveLinksOfTheTripInTheOrderOfTheQueryWithEveryDetail() {
        tripExists();
        Instant inAWeek = NOW.plus(7, ChronoUnit.DAYS);
        ShareLink newer = existingLink(42L, OTHER_LINK_TOKEN, inAWeek, 0, NOW.minusSeconds(60));
        ShareLink older = existingLink(LINK_ID, LINK_TOKEN, null, 12, NOW.minusSeconds(3600));
        linksOfTheTrip(newer, older);

        List<ShareLinkResponse> links = sharingService.listLinks(TRIP_ID);

        // Newest first is the order of the query (ShareLinkRepositoryTest); the service must not reshuffle it
        assertThat(links).extracting(ShareLinkResponse::id).containsExactly(42L, LINK_ID);
        ShareLinkResponse first = links.get(0);
        assertThat(first.token()).isEqualTo(OTHER_LINK_TOKEN);
        assertThat(first.permission()).isEqualTo(SharePermission.VIEW);
        assertThat(first.expiresAt()).isEqualTo(inAWeek);
        assertThat(first.viewCount()).isZero();
        assertThat(first.createdAt()).isEqualTo(NOW.minusSeconds(60));
        assertThat(first.expired()).isFalse();
        ShareLinkResponse second = links.get(1);
        assertThat(second.token()).isEqualTo(LINK_TOKEN);
        assertThat(second.expiresAt()).isNull();
        assertThat(second.viewCount()).isEqualTo(12);
        assertThat(second.expired()).isFalse();
        // Reading the list changes nothing: no row is written and the label of the trip stays
        verify(shareLinkRepository, never()).save(any(ShareLink.class));
        assertThat(trip.getVisibility()).isEqualTo(TripVisibility.PRIVATE);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("expiriesAndWhetherTheyArePast")
    void aListedLinkIsExpiredOnceItsExpiryIsNoLongerAhead(String label, Instant expiresAt, boolean expired) {
        tripExists();
        linksOfTheTrip(existingLink(LINK_ID, LINK_TOKEN, expiresAt, 3, NOW.minusSeconds(3600)));

        List<ShareLinkResponse> links = sharingService.listLinks(TRIP_ID);

        // An expired link is still listed: the owner sees why it stopped working and can revoke it
        assertThat(links).hasSize(1);
        assertThat(links.get(0).expired()).isEqualTo(expired);
        assertThat(links.get(0).expiresAt()).isEqualTo(expiresAt);
    }

    static Stream<Arguments> expiriesAndWhetherTheyArePast() {
        return Stream.of(
                Arguments.of("no expiry", null, false),
                Arguments.of("one microsecond ahead", NOW.plus(1, ChronoUnit.MICROS), false),
                // The boundary of creation: an expiry that is not after "now" is refused there, and expired here
                Arguments.of("exactly now", NOW, true),
                Arguments.of("one microsecond ago", NOW.minus(1, ChronoUnit.MICROS), true),
                Arguments.of("a day ago", NOW.minus(1, ChronoUnit.DAYS), true));
    }

    @Test
    void aTripWithoutLinksHasAnEmptyList() {
        tripExists();
        linksOfTheTrip();

        assertThat(sharingService.listLinks(TRIP_ID)).isEmpty();
    }

    @Test
    void listLinksOnAMissingTripIs404AndReadsNoLink() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.listLinks(TRIP_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(shareLinkRepository);
    }

    @Test
    void revokingALinkStampsItWithTheCurrentInstantAndKeepsTheLabelWhileAnotherLinkIsLeft() {
        tripExists();
        trip.setVisibility(TripVisibility.LINK);
        ShareLink link = linkOfTheTrip(null);
        anotherLinkIsLeft(true);

        sharingService.revokeLink(TRIP_ID, LINK_ID);

        assertThat(link.getRevokedAt()).isEqualTo(NOW);
        verify(shareLinkRepository).save(link);
        // The other link still opens the trip
        assertThat(trip.getVisibility()).isEqualTo(TripVisibility.LINK);
        // What makes the link stays as it was: revoking is not deleting
        assertThat(link.getToken()).isEqualTo(LINK_TOKEN);
        assertThat(link.getViewCount()).isEqualTo(3);
        verifyNoInteractions(tripMemberRepository, mailService);
    }

    @Test
    void revokingTheLastLinkMakesTheTripPrivateAgain() {
        tripExists();
        trip.setVisibility(TripVisibility.LINK);
        ShareLink link = linkOfTheTrip(null);
        anotherLinkIsLeft(false);

        sharingService.revokeLink(TRIP_ID, LINK_ID);

        assertThat(link.getRevokedAt()).isEqualTo(NOW);
        assertThat(trip.getVisibility()).isEqualTo(TripVisibility.PRIVATE);
    }

    @Test
    void anExpiredLinkCanBeRevokedLikeAnyOther() {
        tripExists();
        trip.setVisibility(TripVisibility.LINK);
        ShareLink expired = linkOfTheTrip(NOW.minus(1, ChronoUnit.DAYS));
        anotherLinkIsLeft(false);

        sharingService.revokeLink(TRIP_ID, LINK_ID);

        assertThat(expired.getRevokedAt()).isEqualTo(NOW);
        // The expiry is history and stays; only now does the trip lose its label
        assertThat(expired.getExpiresAt()).isEqualTo(NOW.minus(1, ChronoUnit.DAYS));
        assertThat(trip.getVisibility()).isEqualTo(TripVisibility.PRIVATE);
    }

    @Test
    void revokingALinkASecondTimeIs404AndKeepsTheFirstRevocation() {
        tripExists();
        trip.setVisibility(TripVisibility.LINK);
        ShareLink link = linkOfTheTrip(null);
        Instant firstRevocation = NOW.minus(2, ChronoUnit.HOURS);
        link.revoke(firstRevocation);

        assertThatThrownBy(() -> sharingService.revokeLink(TRIP_ID, LINK_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(link.getRevokedAt()).isEqualTo(firstRevocation);
        verify(shareLinkRepository, never()).save(any(ShareLink.class));
        // A refused call must not touch the label either
        assertThat(trip.getVisibility()).isEqualTo(TripVisibility.LINK);
    }

    @Test
    void revokingALinkThatIsNotOfThisTripIs404AndChangesNothing() {
        tripExists();
        trip.setVisibility(TripVisibility.LINK);
        // The link exists, on another trip: looked up by id + trip, it is not found
        when(shareLinkRepository.findByIdAndTripId(LINK_ID, TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.revokeLink(TRIP_ID, LINK_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(shareLinkRepository, never()).save(any(ShareLink.class));
        assertThat(trip.getVisibility()).isEqualTo(TripVisibility.LINK);
    }

    @Test
    void revokeLinkOnAMissingTripIs404AndReadsNoLink() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.revokeLink(TRIP_ID, LINK_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(shareLinkRepository);
    }

    // ---- helpers -----------------------------------------------------------------------------------------------

    /** A VIEWER row (MEMBER_ID) of friend@example.com on the trip, found by id + trip, in the given status. */
    private TripMember memberOfTrip(MemberStatus status) {
        TripMember row = row("friend@example.com", status,
                status == MemberStatus.PENDING ? null : TestUsers.verified(9L, "friend@example.com"));
        when(tripMemberRepository.findByIdAndTripId(MEMBER_ID, TRIP_ID)).thenReturn(Optional.of(row));
        return row;
    }

    /** A PENDING VIEWER row on the trip for the email, without an account yet, valid until NOW + 3 days. */
    private TripMember pendingInvitation(String email, String rawToken) {
        TripMember row = TripMember.builder()
                .trip(trip)
                .invitedEmail(email)
                .role(MemberRole.VIEWER)
                .inviteTokenHash(SecureTokens.sha256Hex(rawToken))
                .inviteExpiresAt(NOW.plus(3, ChronoUnit.DAYS))
                .invitedBy(owner)
                .invitedAt(NOW.minus(4, ChronoUnit.DAYS))
                .build();
        ReflectionTestUtils.setField(row, "id", MEMBER_ID);
        when(tripMemberRepository.findByInviteTokenHash(SecureTokens.sha256Hex(rawToken))).thenReturn(Optional.of(row));
        return row;
    }

    private void tripExists() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
    }

    private void noRowFor(String email) {
        when(tripMemberRepository.findByTripIdAndInvitedEmail(TRIP_ID, email)).thenReturn(Optional.empty());
    }

    /** {@link #row} plus the lookup by email finding it, for the re-invite cases. */
    private TripMember existingRow(String email, MemberStatus status, User user) {
        TripMember row = row(email, status, user);
        when(tripMemberRepository.findByTripIdAndInvitedEmail(TRIP_ID, email)).thenReturn(Optional.of(row));
        return row;
    }

    /** A VIEWER row of the email on the trip, invited a month ago with token hash "a"*64, in the given status. */
    private TripMember row(String email, MemberStatus status, User user) {
        TripMember row = TripMember.builder()
                .trip(trip)
                .user(user)
                .invitedEmail(email)
                .role(MemberRole.VIEWER)
                .status(status)
                .inviteTokenHash("a".repeat(64))
                .inviteExpiresAt(NOW.minus(23, ChronoUnit.DAYS))
                .invitedBy(owner)
                .invitedAt(NOW.minus(30, ChronoUnit.DAYS))
                .build();
        ReflectionTestUtils.setField(row, "id", MEMBER_ID);
        return row;
    }

    /** An existing row already has its id; the repository hands it back unchanged. */
    private void saveReturnsArgument() {
        when(tripMemberRepository.save(any(TripMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** The database would give the new row an id; the mapper reads it into memberId. */
    private void saveAssignsId() {
        when(tripMemberRepository.save(any(TripMember.class))).thenAnswer(invocation -> {
            TripMember member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", MEMBER_ID);
            return member;
        });
    }

    private TripMember savedMember() {
        ArgumentCaptor<TripMember> captor = ArgumentCaptor.forClass(TripMember.class);
        verify(tripMemberRepository).save(captor.capture());
        return captor.getValue();
    }

    /** The database would give the new link an id and Hibernate its creation time; the mapper reads both. */
    private void linkSaveAssignsId() {
        when(shareLinkRepository.save(any(ShareLink.class))).thenAnswer(invocation -> {
            ShareLink link = invocation.getArgument(0);
            ReflectionTestUtils.setField(link, "id", LINK_ID);
            ReflectionTestUtils.setField(link, "createdAt", NOW);
            return link;
        });
    }

    private ShareLink savedLink() {
        ArgumentCaptor<ShareLink> captor = ArgumentCaptor.forClass(ShareLink.class);
        verify(shareLinkRepository).save(captor.capture());
        return captor.getValue();
    }

    /** A live VIEW link of the trip as the database would hand it back: id and creation time are filled in. */
    private ShareLink existingLink(long id, String token, Instant expiresAt, int viewCount, Instant createdAt) {
        ShareLink link = ShareLink.builder()
                .trip(trip)
                .token(token)
                .expiresAt(expiresAt)
                .viewCount(viewCount)
                .createdBy(owner)
                .build();
        ReflectionTestUtils.setField(link, "id", id);
        ReflectionTestUtils.setField(link, "createdAt", createdAt);
        return link;
    }

    /** LINK_ID is a live link of the trip with 3 views, found by id + trip, for the revoke cases. */
    private ShareLink linkOfTheTrip(Instant expiresAt) {
        ShareLink link = existingLink(LINK_ID, LINK_TOKEN, expiresAt, 3, NOW.minusSeconds(3600));
        when(shareLinkRepository.findByIdAndTripId(LINK_ID, TRIP_ID)).thenReturn(Optional.of(link));
        return link;
    }

    /** Whether the trip has a link that was not revoked besides LINK_ID. */
    private void anotherLinkIsLeft(boolean left) {
        when(shareLinkRepository.existsByTripIdAndRevokedAtIsNullAndIdNot(TRIP_ID, LINK_ID)).thenReturn(left);
    }

    /** What the query of the list returns for the trip: its live links, already newest first. */
    private void linksOfTheTrip(ShareLink... links) {
        when(shareLinkRepository.findByTripIdAndRevokedAtIsNullOrderByCreatedAtDescIdDesc(TRIP_ID))
                .thenReturn(List.of(links));
    }

    private String mailedToken() {
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendInvitationMail(anyString(), anyString(), anyString(), anyBoolean(), anyLong(),
                token.capture());
        return token.getValue();
    }

}
