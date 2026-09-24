package com.trieu.tripplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.VerificationToken;
import com.trieu.tripplanner.model.enums.VerificationTokenType;
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
 * Real MySQL: proves VerificationToken.java matches V4 (validate, ENUM column) and invalidateActive scopes correctly.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class VerificationTokenRepositoryTest {

    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);
    private static final String HASH_C = "c".repeat(64);
    private static final String HASH_D = "d".repeat(64);

    @Autowired
    private VerificationTokenRepository repository;

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
    void saveAndFindByTokenHashRoundTripsEnumAndTimestamps() {
        Instant expiresAt = Instant.now().plus(Duration.ofHours(24)).truncatedTo(ChronoUnit.MICROS);
        repository.saveAndFlush(token(alice, HASH_A, VerificationTokenType.EMAIL_VERIFY, expiresAt));
        entityManager.clear();

        VerificationToken found = repository.findByTokenHash(HASH_A).orElseThrow();

        assertThat(found.getUser().getId()).isEqualTo(alice.getId());
        assertThat(found.getType()).isEqualTo(VerificationTokenType.EMAIL_VERIFY);
        assertThat(found.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(found.getUsedAt()).isNull();
        assertThat(found.isUsable(Instant.now())).isTrue();
        assertThat(repository.findByTokenHash(HASH_D)).isEmpty();
    }

    @Test
    void invalidateActiveOnlyTouchesLiveTokensOfSameUserAndType() {
        Instant future = Instant.now().plus(Duration.ofHours(1));
        repository.save(token(alice, HASH_A, VerificationTokenType.EMAIL_VERIFY, future));   // target
        repository.save(token(alice, HASH_B, VerificationTokenType.PASSWORD_RESET, future)); // other type
        repository.save(token(bob, HASH_C, VerificationTokenType.EMAIL_VERIFY, future));     // other user
        VerificationToken alreadyUsed = token(alice, HASH_D, VerificationTokenType.EMAIL_VERIFY, future);
        alreadyUsed.markUsed(Instant.now().minusSeconds(60));
        repository.save(alreadyUsed);
        repository.flush();

        int invalidated = repository.invalidateActive(alice.getId(), VerificationTokenType.EMAIL_VERIFY, Instant.now());

        assertThat(invalidated).isEqualTo(1);
        assertThat(repository.findByTokenHash(HASH_A).orElseThrow().isUsed()).isTrue();
        assertThat(repository.findByTokenHash(HASH_B).orElseThrow().isUsed()).isFalse();
        assertThat(repository.findByTokenHash(HASH_C).orElseThrow().isUsed()).isFalse();
        assertThat(repository.countByUserIdAndTypeAndUsedAtIsNull(alice.getId(), VerificationTokenType.EMAIL_VERIFY)).isZero();
        assertThat(repository.countByUserIdAndTypeAndUsedAtIsNull(alice.getId(), VerificationTokenType.PASSWORD_RESET)).isEqualTo(1);
    }

    private static User user(String email) {
        return User.builder().email(email).passwordHash("$2a$12$hash").fullName("Test").build();
    }

    private static VerificationToken token(User user, String hash, VerificationTokenType type, Instant expiresAt) {
        return VerificationToken.builder().user(user).tokenHash(hash).type(type).expiresAt(expiresAt).build();
    }

}
