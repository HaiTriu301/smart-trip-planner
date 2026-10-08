package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        when(userRepository.findByEmail("friend@example.com")).thenReturn(Optional.empty());
        saveAssignsId();

        sharingService.invite(TRIP_ID, new InviteMemberRequest("  Friend@Example.COM ", MemberRole.VIEWER));

        assertThat(savedMember().getInvitedEmail()).isEqualTo("friend@example.com");
        verify(mailService).sendInvitationMail(eq("friend@example.com"), anyString(), anyString(), anyBoolean(),
                anyLong(), anyString());
    }

    @Test
    void linksTheAccountWhenTheEmailAlreadyBelongsToOne() {
        tripExists();
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
