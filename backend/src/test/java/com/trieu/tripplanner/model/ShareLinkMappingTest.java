package com.trieu.tripplanner.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.enums.SharePermission;
import com.trieu.tripplanner.repository.ShareLinkRepository;
import java.time.Instant;
import java.time.LocalDate;
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
 * Checks ShareLink.java against V12__create_share_links.sql on a real MySQL (Testcontainers, Flyway,
 * ddl-auto=validate), plus what the sharing rules lean on: one token per link, view links only, links
 * disappearing with their trip, and a view count that saving the entity can never overwrite. Each test runs in
 * a rolled-back transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ShareLinkMappingTest {

    private static final String TOKEN_A = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
    private static final String TOKEN_B = "z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ShareLinkRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User owner;
    private Trip trip;

    @BeforeEach
    void createOwnerAndTrip() {
        owner = entityManager.persist(user("owner@example.com", "Chủ chuyến đi"));
        trip = entityManager.persist(trip(owner, "da-lat-3-ngay-x7k2qp"));
        entityManager.flush();
    }

    @Test
    void everyColumnOfALinkSurvivesARoundTrip() {
        // DATETIME(6) keeps microseconds; nanoseconds would make the comparison flaky (CLAUDE.md section 8)
        Instant expiresAt = Instant.parse("2026-11-01T16:59:59.123456Z");
        Instant revokedAt = Instant.parse("2026-10-20T08:30:00.654321Z");
        ShareLink saved = repository.saveAndFlush(ShareLink.builder()
                .trip(trip)
                .token(TOKEN_A)
                .expiresAt(expiresAt)
                .revokedAt(revokedAt)
                .viewCount(12)
                .createdBy(owner)
                .build());
        entityManager.clear();

        ShareLink found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTrip().getId()).isEqualTo(trip.getId());
        assertThat(found.getToken()).isEqualTo(TOKEN_A);
        assertThat(found.getPermission()).isEqualTo(SharePermission.VIEW);
        assertThat(found.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(found.getRevokedAt()).isEqualTo(revokedAt);
        assertThat(found.getViewCount()).isEqualTo(12);
        assertThat(found.getCreatedBy().getId()).isEqualTo(owner.getId());
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void aNewLinkIsAViewLinkThatNeverExpiresIsLiveAndHasNoViews() {
        ShareLink saved = repository.saveAndFlush(link(TOKEN_A).build());
        entityManager.clear();

        ShareLink found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getPermission()).isEqualTo(SharePermission.VIEW);
        assertThat(found.getExpiresAt()).isNull();
        assertThat(found.getRevokedAt()).isNull();
        assertThat(found.getViewCount()).isZero();
    }

    @Test
    void oneTokenCannotBelongToTwoLinks() {
        Trip otherTrip = entityManager.persistAndFlush(trip(owner, "hue-2-ngay-q1w2e3"));
        repository.saveAndFlush(link(TOKEN_A).build());

        // Even on another trip: the public page finds the trip by the token alone
        assertThatThrownBy(() -> repository.saveAndFlush(
                ShareLink.builder().trip(otherTrip).token(TOKEN_A).createdBy(owner).build()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_share_links_token");
    }

    @Test
    void aTripMayHaveSeveralLinks() {
        repository.saveAndFlush(link(TOKEN_A).build());
        repository.saveAndFlush(link(TOKEN_B).build());

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void aTokenLongerThan32CharactersIsRefused() {
        assertThatThrownBy(() -> repository.saveAndFlush(link(TOKEN_A + "x").build()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Data too long for column 'token'");
    }

    @ParameterizedTest
    @EnumSource(SharePermission.class)
    void everyPermissionIsAcceptedByTheEnumColumn(SharePermission permission) {
        // Guards the day EDIT joins the Java enum without its ALTER (CLAUDE.md section 8)
        ShareLink saved = repository.saveAndFlush(link(TOKEN_A).permission(permission).build());
        entityManager.clear();

        assertThat(repository.findById(saved.getId()).orElseThrow().getPermission()).isEqualTo(permission);
    }

    @Test
    void editIsNotAColumnValueOfPermissionYet() {
        // Decision 3 of the Phase 4 review: a link only lets people view, so the ENUM must refuse anything else
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO share_links (trip_id, token, permission, created_by, created_at, updated_at)
                VALUES (?, ?, 'EDIT', ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, trip.getId(), TOKEN_A, owner.getId()))
                .hasMessageContaining("Data truncated for column 'permission'");
    }

    @Test
    void hardDeletingTheTripRemovesItsLinks() {
        repository.saveAndFlush(link(TOKEN_A).build());
        repository.saveAndFlush(link(TOKEN_B).revokedAt(Instant.parse("2026-10-20T00:00:00Z")).build());

        // What the retention scheduler will do to a soft-deleted trip (rule 14.8); bypasses @SQLDelete on purpose
        jdbcTemplate.update("DELETE FROM trips WHERE id = ?", trip.getId());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM share_links", Integer.class)).isZero();
    }

    @Test
    void whatMakesALinkCannotBeChangedAfterCreation() {
        User stranger = entityManager.persist(user("stranger@example.com", "Người lạ"));
        Trip otherTrip = entityManager.persistAndFlush(trip(owner, "vung-tau-r4t5y6"));
        Instant expiresAt = Instant.parse("2026-11-01T00:00:00Z");
        Instant revokedAt = Instant.parse("2026-10-20T00:00:00Z");
        ShareLink saved = repository.saveAndFlush(link(TOKEN_A).expiresAt(expiresAt).build());

        // No setters exist; even a reflective change must not reach the database (updatable = false)
        ReflectionTestUtils.setField(saved, "trip", otherTrip);
        ReflectionTestUtils.setField(saved, "token", TOKEN_B);
        ReflectionTestUtils.setField(saved, "expiresAt", null);
        ReflectionTestUtils.setField(saved, "createdBy", stranger);
        ReflectionTestUtils.setField(saved, "revokedAt", revokedAt); // the one updatable column: forces an UPDATE
        repository.saveAndFlush(saved);
        entityManager.clear();

        ShareLink found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTrip().getId()).isEqualTo(trip.getId());
        assertThat(found.getToken()).isEqualTo(TOKEN_A);
        assertThat(found.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(found.getCreatedBy().getId()).isEqualTo(owner.getId());
        assertThat(found.getRevokedAt()).isEqualTo(revokedAt);
    }

    @Test
    void savingTheEntityNeverWritesBackAnOlderViewCount() {
        ShareLink loaded = repository.saveAndFlush(link(TOKEN_A).build());
        // Seven people open the public page while the owner still holds the link loaded with 0 views
        jdbcTemplate.update("UPDATE share_links SET view_count = view_count + 7 WHERE id = ?", loaded.getId());

        ReflectionTestUtils.setField(loaded, "revokedAt", Instant.parse("2026-10-20T00:00:00Z"));
        repository.saveAndFlush(loaded);
        entityManager.clear();

        assertThat(repository.findById(loaded.getId()).orElseThrow().getViewCount()).isEqualTo(7);
    }

    /** A live view link on the shared trip, created by the owner, that never expires. */
    private ShareLink.ShareLinkBuilder link(String token) {
        return ShareLink.builder()
                .trip(trip)
                .token(token)
                .createdBy(owner);
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
