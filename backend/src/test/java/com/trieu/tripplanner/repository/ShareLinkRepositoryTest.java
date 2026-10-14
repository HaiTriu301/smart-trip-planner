package com.trieu.tripplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.ShareLink;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Real MySQL (Testcontainers): the list of links is filtered and ordered SQL that only a real database can
 * prove. Each test runs in a rolled-back transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ShareLinkRepositoryTest {

    private static final String TOKEN_A = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
    private static final String TOKEN_B = "b1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
    private static final String TOKEN_C = "c1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
    private static final String TOKEN_D = "d1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
    // Whole microseconds: DATETIME(6) would round anything finer (CLAUDE.md section 8)
    private static final Instant REVOKED_AT = Instant.parse("2026-10-10T08:30:00.654321Z");

    @Autowired
    private ShareLinkRepository shareLinkRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User owner;
    private Trip trip;

    @BeforeEach
    void createOwnerAndTrip() {
        owner = entityManager.persist(user("owner@example.com"));
        trip = entityManager.persist(trip(owner, "da-lat-aaaaaa"));
    }

    @Test
    void listsTheLinksOfATripNewestFirst() {
        // Stored in an order that is neither the expected one nor its reverse
        ShareLink middle = entityManager.persist(link(TOKEN_A).build());
        ShareLink newest = entityManager.persist(link(TOKEN_B).build());
        ShareLink oldest = entityManager.persist(link(TOKEN_C).build());
        entityManager.flush();
        createdMinutesAgo(middle, 20);
        createdMinutesAgo(newest, 10);
        createdMinutesAgo(oldest, 30);
        entityManager.clear();

        List<ShareLink> links = liveLinksOf(trip);

        assertThat(links).extracting(ShareLink::getToken).containsExactly(TOKEN_B, TOKEN_A, TOKEN_C);
    }

    @Test
    void linksCreatedAtTheSameInstantComeOutByIdNewestFirst() {
        ShareLink first = entityManager.persist(link(TOKEN_A).build());
        ShareLink second = entityManager.persist(link(TOKEN_B).build());
        ShareLink third = entityManager.persist(link(TOKEN_C).build());
        entityManager.flush();
        createdMinutesAgo(first, 10);
        createdMinutesAgo(second, 10);
        createdMinutesAgo(third, 10);
        entityManager.clear();

        // Without the second sort key MySQL may hand equal rows back in any order, differently from call to call
        assertThat(liveLinksOf(trip)).extracting(ShareLink::getId)
                .containsExactly(third.getId(), second.getId(), first.getId());
    }

    @Test
    void hidesRevokedLinksButKeepsExpiredOnes() {
        entityManager.persist(link(TOKEN_A).build());
        entityManager.persist(link(TOKEN_B).revokedAt(Instant.parse("2026-10-10T00:00:00Z")).build());
        // Expired long ago and never revoked: still the owner's to see and to revoke
        entityManager.persist(link(TOKEN_C).expiresAt(Instant.parse("2020-01-01T00:00:00Z")).build());
        // Revoked after it had expired: revoked wins
        entityManager.persist(link(TOKEN_D).expiresAt(Instant.parse("2020-01-01T00:00:00Z"))
                .revokedAt(Instant.parse("2026-10-10T00:00:00Z")).build());
        entityManager.flush();
        entityManager.clear();

        assertThat(liveLinksOf(trip)).extracting(ShareLink::getToken).containsExactlyInAnyOrder(TOKEN_A, TOKEN_C);
    }

    @Test
    void ignoresTheLinksOfOtherTrips() {
        Trip other = entityManager.persist(trip(owner, "hue-bbbbbb"));
        entityManager.persist(link(TOKEN_A).build());
        entityManager.persist(ShareLink.builder().trip(other).token(TOKEN_B).createdBy(owner).build());
        entityManager.flush();
        entityManager.clear();

        assertThat(liveLinksOf(trip)).extracting(ShareLink::getToken).containsExactly(TOKEN_A);
        assertThat(liveLinksOf(other)).extracting(ShareLink::getToken).containsExactly(TOKEN_B);
        assertThat(shareLinkRepository.findByTripIdAndRevokedAtIsNullOrderByCreatedAtDescIdDesc(999_999L)).isEmpty();
    }

    @Test
    void findByIdAndTripIdFindsALinkOnlyUnderItsOwnTrip() {
        Trip other = entityManager.persist(trip(owner, "hue-bbbbbb"));
        ShareLink mine = entityManager.persist(link(TOKEN_A).build());
        ShareLink revoked = entityManager.persist(link(TOKEN_B).revokedAt(REVOKED_AT).build());
        entityManager.flush();
        entityManager.clear();

        assertThat(shareLinkRepository.findByIdAndTripId(mine.getId(), trip.getId()))
                .get().extracting(ShareLink::getToken).isEqualTo(TOKEN_A);
        // The right id under the wrong trip: an owner cannot reach into somebody else's trip by guessing ids
        assertThat(shareLinkRepository.findByIdAndTripId(mine.getId(), other.getId())).isEmpty();
        // A revoked link is found too: answering 404 for it is the decision of the service
        assertThat(shareLinkRepository.findByIdAndTripId(revoked.getId(), trip.getId()))
                .get().extracting(ShareLink::getRevokedAt).isEqualTo(REVOKED_AT);
        assertThat(shareLinkRepository.findByIdAndTripId(999_999L, trip.getId())).isEmpty();
    }

    @Test
    void aTripWhoseOtherLinksAreAllRevokedHasNoLinkLeftBesidesTheGivenOne() {
        Trip other = entityManager.persist(trip(owner, "hue-bbbbbb"));
        ShareLink last = entityManager.persist(link(TOKEN_A).build());
        entityManager.persist(link(TOKEN_B).revokedAt(REVOKED_AT).build());
        // A live link of another trip does not keep this trip shared
        entityManager.persist(ShareLink.builder().trip(other).token(TOKEN_C).createdBy(owner).build());
        entityManager.flush();
        entityManager.clear();

        assertThat(shareLinkRepository.existsByTripIdAndRevokedAtIsNullAndIdNot(trip.getId(), last.getId()))
                .isFalse();
    }

    @Test
    void aSecondLiveLinkIsLeftBesidesTheGivenOneEvenWhenItHasExpired() {
        ShareLink first = entityManager.persist(link(TOKEN_A).build());
        // Expired and never revoked: it still keeps the LINK label of the trip (design.md 10.2)
        ShareLink expired = entityManager.persist(link(TOKEN_B).expiresAt(Instant.parse("2020-01-01T00:00:00Z"))
                .build());
        entityManager.flush();
        entityManager.clear();

        assertThat(shareLinkRepository.existsByTripIdAndRevokedAtIsNullAndIdNot(trip.getId(), first.getId()))
                .isTrue();
        assertThat(shareLinkRepository.existsByTripIdAndRevokedAtIsNullAndIdNot(trip.getId(), expired.getId()))
                .isTrue();
    }

    @Test
    void revokingThroughTheEntityWritesTheInstantAndTakesTheLinkOutOfTheList() {
        ShareLink kept = entityManager.persist(link(TOKEN_A).build());
        ShareLink gone = entityManager.persist(link(TOKEN_B).build());
        entityManager.flush();
        entityManager.clear();

        ShareLink loaded = shareLinkRepository.findByIdAndTripId(gone.getId(), trip.getId()).orElseThrow();
        loaded.revoke(REVOKED_AT);
        shareLinkRepository.saveAndFlush(loaded);
        entityManager.clear();

        assertThat(shareLinkRepository.findById(gone.getId()).orElseThrow().getRevokedAt()).isEqualTo(REVOKED_AT);
        assertThat(liveLinksOf(trip)).extracting(ShareLink::getId).containsExactly(kept.getId());
        // The row is still there: revoking never deletes
        assertThat(shareLinkRepository.count()).isEqualTo(2);
    }

    private List<ShareLink> liveLinksOf(Trip ofTrip) {
        return shareLinkRepository.findByTripIdAndRevokedAtIsNullOrderByCreatedAtDescIdDesc(ofTrip.getId());
    }

    /**
     * created_at is filled by Hibernate and not updatable, so the test moves it in SQL. The time is worked out by
     * MySQL in UTC: a java.sql.Timestamp would be written in the time zone of the JVM (CLAUDE.md section 8).
     */
    private void createdMinutesAgo(ShareLink link, int minutes) {
        jdbcTemplate.update("""
                UPDATE share_links SET created_at = TIMESTAMP('2026-10-13 03:00:00') - INTERVAL ? MINUTE WHERE id = ?
                """, minutes, link.getId());
    }

    /** A live view link on the shared trip, created by the owner, that never expires. */
    private ShareLink.ShareLinkBuilder link(String token) {
        return ShareLink.builder()
                .trip(trip)
                .token(token)
                .createdBy(owner);
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
