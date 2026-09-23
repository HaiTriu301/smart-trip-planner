package com.trieu.tripplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.RefreshToken;
import com.trieu.tripplanner.model.User;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Real MySQL: proves RefreshToken.java matches V3 (validate) and the custom queries do what they claim.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RefreshTokenRepositoryTest {

    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);
    private static final String HASH_C = "c".repeat(64);

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = userRepository.saveAndFlush(user("alice@example.com"));
        bob = userRepository.saveAndFlush(user("bob@example.com"));
    }

    @Test
    void saveAndFindByTokenHashRoundTripsAllColumns() {
        // DATETIME(6) keeps microseconds and ROUNDS; a JVM Instant carries nanoseconds → truncate before comparing
        Instant expiresAt = Instant.now().plus(Duration.ofDays(7)).truncatedTo(ChronoUnit.MICROS);
        RefreshToken saved = refreshTokenRepository.saveAndFlush(token(alice, HASH_A, expiresAt, "Mozilla/5.0", "203.0.113.9"));
        entityManager.clear();

        RefreshToken found = refreshTokenRepository.findByTokenHash(HASH_A).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getUser().getId()).isEqualTo(alice.getId());
        assertThat(found.getTokenHash()).isEqualTo(HASH_A);
        assertThat(found.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(found.getRevokedAt()).isNull();
        assertThat(found.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(found.getIpAddress()).isEqualTo("203.0.113.9");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.isActive(Instant.now())).isTrue();
    }

    @Test
    void findByTokenHashIsEmptyForUnknownHash() {
        assertThat(refreshTokenRepository.findByTokenHash(HASH_C)).isEmpty();
    }

    @Test
    void revokeAllActiveByUserIdOnlyTouchesThatUsersLiveSessions() {
        refreshTokenRepository.save(token(alice, HASH_A, Instant.now().plus(Duration.ofDays(7)), null, null));
        RefreshToken alreadyRevoked = token(alice, HASH_B, Instant.now().plus(Duration.ofDays(7)), null, null);
        alreadyRevoked.revoke(Instant.now().minus(Duration.ofHours(1)));
        refreshTokenRepository.save(alreadyRevoked);
        refreshTokenRepository.save(token(bob, HASH_C, Instant.now().plus(Duration.ofDays(7)), null, null));
        refreshTokenRepository.flush();

        int revoked = refreshTokenRepository.revokeAllActiveByUserId(alice.getId(), Instant.now());

        assertThat(revoked).isEqualTo(1);
        assertThat(refreshTokenRepository.findByTokenHash(HASH_A).orElseThrow().isRevoked()).isTrue();
        assertThat(refreshTokenRepository.findByTokenHash(HASH_C).orElseThrow().isRevoked()).isFalse();
        assertThat(refreshTokenRepository.countByUserIdAndRevokedAtIsNull(alice.getId())).isZero();
        assertThat(refreshTokenRepository.countByUserIdAndRevokedAtIsNull(bob.getId())).isEqualTo(1);
    }

    @Test
    void revokeKeepsTheFirstRevocationTime() {
        RefreshToken token = refreshTokenRepository.saveAndFlush(
                token(alice, HASH_A, Instant.now().plus(Duration.ofDays(7)), null, null));
        Instant first = Instant.parse("2026-01-01T00:00:00Z");

        token.revoke(first);
        token.revoke(first.plusSeconds(60));
        refreshTokenRepository.flush();
        entityManager.clear();

        assertThat(refreshTokenRepository.findById(token.getId()).orElseThrow().getRevokedAt()).isEqualTo(first);
    }

    private static User user(String email) {
        return User.builder().email(email).passwordHash("$2a$12$hash").fullName("Test").build();
    }

    private static RefreshToken token(User user, String hash, Instant expiresAt, String userAgent, String ip) {
        return RefreshToken.builder().user(user).tokenHash(hash).expiresAt(expiresAt).userAgent(userAgent).ipAddress(ip).build();
    }

}
