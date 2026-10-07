package com.trieu.tripplanner.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.model.enums.MemberStatus;
import com.trieu.tripplanner.repository.TripMemberRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Checks TripMember.java against V11__create_trip_members.sql on a real MySQL (Testcontainers, Flyway,
 * ddl-auto=validate), plus the keys the sharing rules lean on: one row per account and per email on a trip,
 * one token per invitation, and members disappearing with their trip. Each test runs in a rolled-back
 * transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripMemberMappingTest {

    private static final String SHA256_HEX_A = "a".repeat(64);
    private static final String SHA256_HEX_B = "b".repeat(64);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TripMemberRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User owner;
    private User guest;
    private Trip trip;

    @BeforeEach
    void createOwnerGuestAndTrip() {
        owner = entityManager.persist(user("owner@example.com", "Chủ chuyến đi"));
        guest = entityManager.persist(user("guest@example.com", "Khách mời"));
        trip = entityManager.persist(trip(owner, "da-lat-3-ngay-x7k2qp"));
        entityManager.flush();
    }

    @Test
    void everyColumnOfAPendingInvitationSurvivesARoundTrip() {
        // DATETIME(6) keeps microseconds; nanoseconds would make the comparison flaky (CLAUDE.md section 8)
        Instant invitedAt = Instant.parse("2026-10-07T08:30:00.123456Z");
        Instant expiresAt = invitedAt.plus(7, ChronoUnit.DAYS);
        TripMember saved = repository.saveAndFlush(TripMember.builder()
                .trip(trip)
                .invitedEmail("friend@example.com")
                .role(MemberRole.EDITOR)
                .inviteTokenHash(SHA256_HEX_A)
                .inviteExpiresAt(expiresAt)
                .invitedBy(owner)
                .invitedAt(invitedAt)
                .build());
        entityManager.clear();

        TripMember found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTrip().getId()).isEqualTo(trip.getId());
        assertThat(found.getUser()).isNull();
        assertThat(found.getInvitedEmail()).isEqualTo("friend@example.com");
        assertThat(found.getRole()).isEqualTo(MemberRole.EDITOR);
        assertThat(found.getStatus()).isEqualTo(MemberStatus.PENDING);
        assertThat(found.getInviteTokenHash()).isEqualTo(SHA256_HEX_A);
        assertThat(found.getInviteExpiresAt()).isEqualTo(expiresAt);
        assertThat(found.getInvitedBy().getId()).isEqualTo(owner.getId());
        assertThat(found.getInvitedAt()).isEqualTo(invitedAt);
        assertThat(found.getAcceptedAt()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void anAcceptedMemberKeepsItsUserAndAcceptedAtAndNoToken() {
        Instant acceptedAt = Instant.parse("2026-10-08T09:00:00Z");
        TripMember saved = repository.saveAndFlush(invite("guest@example.com")
                .user(guest)
                .status(MemberStatus.ACCEPTED)
                .inviteTokenHash(null)
                .inviteExpiresAt(null)
                .acceptedAt(acceptedAt)
                .build());
        entityManager.clear();

        TripMember found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getUser().getId()).isEqualTo(guest.getId());
        assertThat(found.getStatus()).isEqualTo(MemberStatus.ACCEPTED);
        assertThat(found.getInviteTokenHash()).isNull();
        assertThat(found.getInviteExpiresAt()).isNull();
        assertThat(found.getAcceptedAt()).isEqualTo(acceptedAt);
    }

    @Test
    void theSameAccountCannotBeAMemberOfTheSameTripTwice() {
        repository.saveAndFlush(invite("guest@example.com").user(guest).status(MemberStatus.ACCEPTED).build());

        assertThatThrownBy(() -> repository.saveAndFlush(
                invite("other-spelling@example.com").user(guest).inviteTokenHash(SHA256_HEX_B).build()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_trip_members_trip_user");
    }

    @Test
    void theSameEmailCannotBeInvitedToTheSameTripTwice() {
        repository.saveAndFlush(invite("friend@example.com").build());

        assertThatThrownBy(() -> repository.saveAndFlush(
                invite("friend@example.com").inviteTokenHash(SHA256_HEX_B).build()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_trip_members_trip_email");
    }

    @Test
    void theSameEmailMayBeInvitedToTwoDifferentTrips() {
        Trip otherTrip = entityManager.persistAndFlush(trip(owner, "hue-2-ngay-q1w2e3"));

        repository.saveAndFlush(invite("friend@example.com").build());
        repository.saveAndFlush(TripMember.builder()
                .trip(otherTrip)
                .invitedEmail("friend@example.com")
                .role(MemberRole.VIEWER)
                .inviteTokenHash(SHA256_HEX_B)
                .inviteExpiresAt(Instant.parse("2026-10-14T00:00:00Z"))
                .invitedBy(owner)
                .invitedAt(Instant.parse("2026-10-07T00:00:00Z"))
                .build());

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void severalPendingInvitationsToPeopleWithoutAnAccountDoNotCollide() {
        // user_id is NULL on both rows; MySQL does not count NULLs as duplicates in a UNIQUE key
        repository.saveAndFlush(invite("first@example.com").inviteTokenHash(SHA256_HEX_A).build());
        repository.saveAndFlush(invite("second@example.com").inviteTokenHash(SHA256_HEX_B).build());

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void oneInvitationTokenCannotBelongToTwoRows() {
        Trip otherTrip = entityManager.persistAndFlush(trip(owner, "sa-pa-u7i8o9"));
        repository.saveAndFlush(invite("friend@example.com").build());

        assertThatThrownBy(() -> repository.saveAndFlush(TripMember.builder()
                .trip(otherTrip)
                .invitedEmail("someone-else@example.com")
                .role(MemberRole.VIEWER)
                .inviteTokenHash(SHA256_HEX_A)
                .inviteExpiresAt(Instant.parse("2026-10-14T00:00:00Z"))
                .invitedBy(owner)
                .invitedAt(Instant.parse("2026-10-07T00:00:00Z"))
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_trip_members_invite_token_hash");
    }

    @Test
    void acceptedRowsWithoutATokenDoNotCollideOnTheTokenKey() {
        User third = entityManager.persist(user("third@example.com", "Người thứ ba"));
        repository.saveAndFlush(invite("guest@example.com").user(guest).status(MemberStatus.ACCEPTED)
                .inviteTokenHash(null).inviteExpiresAt(null).build());
        repository.saveAndFlush(invite("third@example.com").user(third).status(MemberStatus.ACCEPTED)
                .inviteTokenHash(null).inviteExpiresAt(null).build());

        assertThat(repository.count()).isEqualTo(2);
    }

    @ParameterizedTest
    @EnumSource(MemberRole.class)
    void everyRoleIsAcceptedByTheEnumColumn(MemberRole role) {
        TripMember saved = repository.saveAndFlush(invite("friend@example.com").role(role).build());
        entityManager.clear();

        assertThat(repository.findById(saved.getId()).orElseThrow().getRole()).isEqualTo(role);
    }

    @ParameterizedTest
    @EnumSource(MemberStatus.class)
    void everyStatusIsAcceptedByTheEnumColumn(MemberStatus status) {
        TripMember saved = repository.saveAndFlush(invite("friend@example.com").status(status).build());
        entityManager.clear();

        assertThat(repository.findById(saved.getId()).orElseThrow().getStatus()).isEqualTo(status);
    }

    @Test
    void ownerIsNotAColumnValueOfRole() {
        // Decision 2 of the Phase 4 review: the owner has no member row, so the ENUM must refuse the value
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO trip_members (trip_id, invited_email, role, invited_by, invited_at, created_at, updated_at)
                VALUES (?, 'owner@example.com', 'OWNER', ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, trip.getId(), owner.getId()))
                .hasMessageContaining("Data truncated for column 'role'");
    }

    @Test
    void hardDeletingTheTripRemovesItsMembers() {
        repository.saveAndFlush(invite("friend@example.com").build());
        repository.saveAndFlush(invite("guest@example.com").user(guest).status(MemberStatus.ACCEPTED)
                .inviteTokenHash(null).inviteExpiresAt(null).build());

        // What the retention scheduler will do to a soft-deleted trip (rule 14.8); bypasses @SQLDelete on purpose
        jdbcTemplate.update("DELETE FROM trips WHERE id = ?", trip.getId());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trip_members", Integer.class)).isZero();
    }

    @Test
    void tripInvitedEmailAndInviterCannotBeChangedAfterCreation() {
        Trip otherTrip = entityManager.persistAndFlush(trip(owner, "vung-tau-r4t5y6"));
        TripMember saved = repository.saveAndFlush(invite("friend@example.com").build());

        // No setters exist; even a reflective change must not reach the database (updatable = false)
        ReflectionTestUtils.setField(saved, "trip", otherTrip);
        ReflectionTestUtils.setField(saved, "invitedEmail", "changed@example.com");
        ReflectionTestUtils.setField(saved, "invitedBy", guest);
        saved.setRole(MemberRole.VIEWER); // make the row dirty so an UPDATE is issued
        repository.saveAndFlush(saved);
        entityManager.clear();

        TripMember found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTrip().getId()).isEqualTo(trip.getId());
        assertThat(found.getInvitedEmail()).isEqualTo("friend@example.com");
        assertThat(found.getInvitedBy().getId()).isEqualTo(owner.getId());
        assertThat(found.getRole()).isEqualTo(MemberRole.VIEWER);
    }

    /** A pending VIEWER invitation on the shared trip, sent by the owner, with token A. */
    private TripMember.TripMemberBuilder invite(String email) {
        return TripMember.builder()
                .trip(trip)
                .invitedEmail(email)
                .role(MemberRole.VIEWER)
                .inviteTokenHash(SHA256_HEX_A)
                .inviteExpiresAt(Instant.parse("2026-10-14T00:00:00Z"))
                .invitedBy(owner)
                .invitedAt(Instant.parse("2026-10-07T00:00:00Z"));
    }

    private static User user(String email, String fullName) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName(fullName)
                .build();
    }

    private static Trip trip(User owner, String slug) {
        return Trip.builder()
                .owner(owner)
                .title("Chuyến đi thử")
                .slug(slug)
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 3))
                .build();
    }

}
