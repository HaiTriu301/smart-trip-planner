package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.common.util.SecureTokens;
import com.trieu.tripplanner.dto.request.InviteMemberRequest;
import com.trieu.tripplanner.dto.response.MemberResponse;
import com.trieu.tripplanner.dto.response.TripRole;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.FieldViolation;
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
import com.trieu.tripplanner.support.TestUsers;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private static final Instant NOW = Instant.parse("2026-10-08T03:00:00Z");

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMemberRepository tripMemberRepository;

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
        sharingService = new SharingServiceImpl(tripRepository, tripMemberRepository, userRepository,
                Mappers.getMapper(MemberMapper.class), mailService, Clock.fixed(NOW, ZoneOffset.UTC));
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

    // ---- helpers -----------------------------------------------------------------------------------------------

    private void tripExists() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
    }

    private void noRowFor(String email) {
        when(tripMemberRepository.findByTripIdAndInvitedEmail(TRIP_ID, email)).thenReturn(Optional.empty());
    }

    /** A VIEWER row of the email on the trip, invited a month ago with token hash "a"*64, in the given status. */
    private TripMember existingRow(String email, MemberStatus status, User user) {
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
        when(tripMemberRepository.findByTripIdAndInvitedEmail(TRIP_ID, email)).thenReturn(Optional.of(row));
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

    private String mailedToken() {
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendInvitationMail(anyString(), anyString(), anyString(), anyBoolean(), anyLong(),
                token.capture());
        return token.getValue();
    }

}
