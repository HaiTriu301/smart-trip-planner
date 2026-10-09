package com.trieu.tripplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripMember;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.model.enums.MemberStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Real MySQL (Testcontainers): the member list is ordered SQL that only a real database can prove.
 * Each test runs in a rolled-back transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripMemberRepositoryTest {

    private static final Instant SEPT_1 = Instant.parse("2026-09-01T00:00:00Z");

    @Autowired
    private TripMemberRepository tripMemberRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User owner;
    private Trip trip;

    @BeforeEach
    void createOwnerAndTrip() {
        owner = entityManager.persist(user("owner@example.com"));
        trip = entityManager.persist(trip(owner, "da-lat-aaaaaa"));
    }

    @Test
    void findActiveByTripIdListsAcceptedBeforePendingEachByInvitationTimeAndHidesRemoved() {
        User early = entityManager.persist(user("early@example.com"));
        User late = entityManager.persist(user("late@example.com"));
        User gone = entityManager.persist(user("gone@example.com"));
        entityManager.persist(member("waiting-late@example.com", null, MemberStatus.PENDING, SEPT_1.plusSeconds(300)));
        entityManager.persist(member("late@example.com", late, MemberStatus.ACCEPTED, SEPT_1.plusSeconds(200)));
        entityManager.persist(member("gone@example.com", gone, MemberStatus.REMOVED, SEPT_1));
        entityManager.persist(member("waiting-early@example.com", null, MemberStatus.PENDING, SEPT_1.plusSeconds(100)));
        entityManager.persist(member("early@example.com", early, MemberStatus.ACCEPTED, SEPT_1.plusSeconds(50)));
        entityManager.flush();
        entityManager.clear();

        List<TripMember> members = tripMemberRepository.findActiveByTripId(trip.getId());

        assertThat(members).extracting(TripMember::getInvitedEmail).containsExactly(
                "early@example.com", "late@example.com", "waiting-early@example.com", "waiting-late@example.com");
    }

    @Test
    void findActiveByTripIdFetchesTheAccountInTheSameQuery() {
        User joined = entityManager.persist(user("joined@example.com"));
        entityManager.persist(member("joined@example.com", joined, MemberStatus.ACCEPTED, SEPT_1));
        entityManager.persist(member("waiting@example.com", null, MemberStatus.PENDING, SEPT_1.plusSeconds(10)));
        entityManager.flush();
        entityManager.clear();

        List<TripMember> members = tripMemberRepository.findActiveByTripId(trip.getId());

        // Initialised already: no second SELECT when the mapper reads the name (CLAUDE.md section 8, N+1)
        assertThat(Hibernate.isInitialized(members.get(0).getUser())).isTrue();
        assertThat(members.get(0).getUser().getFullName()).isEqualTo("Test joined@example.com");
        assertThat(members.get(1).getUser()).isNull();
    }

    @Test
    void findActiveByTripIdIgnoresTheMembersOfOtherTrips() {
        Trip other = entityManager.persist(trip(owner, "hue-bbbbbb"));
        entityManager.persist(TripMember.builder().trip(other).invitedEmail("elsewhere@example.com")
                .role(MemberRole.VIEWER).invitedBy(owner).invitedAt(SEPT_1).build());
        entityManager.persist(member("here@example.com", null, MemberStatus.PENDING, SEPT_1));
        entityManager.flush();

        assertThat(tripMemberRepository.findActiveByTripId(trip.getId()))
                .extracting(TripMember::getInvitedEmail).containsExactly("here@example.com");
        assertThat(tripMemberRepository.findActiveByTripId(999_999L)).isEmpty();
    }

    private TripMember member(String email, User user, MemberStatus status, Instant invitedAt) {
        return TripMember.builder()
                .trip(trip)
                .user(user)
                .invitedEmail(email)
                .role(MemberRole.VIEWER)
                .status(status)
                .inviteTokenHash(status == MemberStatus.PENDING ? "hash-" + email : null)
                .invitedBy(owner)
                .invitedAt(invitedAt)
                .build();
    }

    private static User user(String email) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName("Test " + email)
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
