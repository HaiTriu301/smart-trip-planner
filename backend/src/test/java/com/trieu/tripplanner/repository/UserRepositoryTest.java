package com.trieu.tripplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.Plan;
import com.trieu.tripplanner.model.enums.Role;
import com.trieu.tripplanner.model.enums.UserStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs against a real MySQL (Testcontainers) with the Flyway schema and ddl-auto=validate,
 * so a mismatch between User.java and V2__create_users_table.sql fails here, not at first deploy.
 * Each test runs in a transaction that is rolled back, so tests do not see each other's rows.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savePersistsRowAndAppliesDefaults() {
        User saved = userRepository.saveAndFlush(newUser("an@example.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getPlan()).isEqualTo(Plan.FREE);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.getTimezone()).isEqualTo(User.DEFAULT_TIMEZONE);
        assertThat(saved.getLocale()).isEqualTo(User.DEFAULT_LOCALE);
        assertThat(saved.getDeletedAt()).isNull();
    }

    @Test
    void findByEmailReturnsUserAndReadsEnumsBackFromMysqlEnumColumns() {
        User saved = userRepository.saveAndFlush(User.builder()
                .email("admin@example.com")
                .passwordHash("$2a$12$hash")
                .fullName("Quản trị")
                .role(Role.ADMIN)
                .plan(Plan.PREMIUM)
                .status(UserStatus.BLOCKED)
                .planExpiresAt(Instant.parse("2030-01-01T00:00:00Z"))
                .emailVerified(true)
                .build());
        entityManager.clear(); // force a real SELECT instead of returning the cached instance

        Optional<User> found = userRepository.findByEmail("admin@example.com");

        assertThat(found).isPresent();
        User user = found.get();
        assertThat(user.getId()).isEqualTo(saved.getId());
        assertThat(user.getFullName()).isEqualTo("Quản trị");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.getPlan()).isEqualTo(Plan.PREMIUM);
        assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(user.getPlanExpiresAt()).isEqualTo(Instant.parse("2030-01-01T00:00:00Z"));
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void findByEmailReturnsEmptyForUnknownEmail() {
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void emailIsTrimmedAndStoredLowercase() {
        User saved = userRepository.saveAndFlush(newUser("  Binh.Tran@Example.COM "));

        // Read the raw column: the utf8mb4_unicode_ci collation makes JPA lookups case-insensitive anyway,
        // so only the stored value proves that @PrePersist normalized it
        String stored = jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE id = ?", String.class, saved.getId());
        assertThat(stored).isEqualTo("binh.tran@example.com");
        assertThat(saved.getEmail()).isEqualTo("binh.tran@example.com");
        assertThat(userRepository.findByEmail("binh.tran@example.com")).isPresent();
    }

    @Test
    void existsByEmailReflectsPresence() {
        assertThat(userRepository.existsByEmail("chi@example.com")).isFalse();

        userRepository.saveAndFlush(newUser("chi@example.com"));

        assertThat(userRepository.existsByEmail("chi@example.com")).isTrue();
    }

    @Test
    void duplicateEmailIsRejectedByUniqueIndex() {
        userRepository.saveAndFlush(newUser("dup@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("DUP@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteIsSoftAndHidesRowFromQueries() {
        User saved = userRepository.saveAndFlush(newUser("gone@example.com"));
        Long id = saved.getId();

        userRepository.delete(saved);
        userRepository.flush();
        entityManager.clear();

        // Invisible through JPA...
        assertThat(userRepository.findById(id)).isEmpty();
        assertThat(userRepository.findByEmail("gone@example.com")).isEmpty();
        assertThat(userRepository.existsByEmail("gone@example.com")).isFalse();

        // ...but the row is still there with deleted_at stamped (JDBC hands DATETIME back as LocalDateTime)
        LocalDateTime deletedAt = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM users WHERE id = ?", LocalDateTime.class, id);
        assertThat(deletedAt).isNotNull();
    }

    @Test
    void updateChangesColumnsAndKeepsCreatedAt() {
        User saved = userRepository.saveAndFlush(newUser("dung@example.com"));
        Instant createdAt = saved.getCreatedAt();

        saved.setFullName("Tên mới");
        saved.setPlan(Plan.PREMIUM);
        userRepository.saveAndFlush(saved);
        entityManager.clear();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getFullName()).isEqualTo("Tên mới");
        assertThat(reloaded.getPlan()).isEqualTo(Plan.PREMIUM);
        assertThat(reloaded.getCreatedAt()).isEqualTo(createdAt);
        assertThat(reloaded.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }

    @Test
    void isPremiumHonoursExpiry() {
        User active = User.builder().email("a@x.com").passwordHash("h").fullName("A")
                .plan(Plan.PREMIUM).planExpiresAt(Instant.now().plusSeconds(3600)).build();
        User expired = User.builder().email("b@x.com").passwordHash("h").fullName("B")
                .plan(Plan.PREMIUM).planExpiresAt(Instant.now().minusSeconds(3600)).build();
        User lifetime = User.builder().email("c@x.com").passwordHash("h").fullName("C")
                .plan(Plan.PREMIUM).build();
        User free = newUser("d@x.com");

        assertThat(active.isPremium()).isTrue();
        assertThat(expired.isPremium()).isFalse();
        assertThat(lifetime.isPremium()).isTrue();
        assertThat(free.isPremium()).isFalse();
    }

    private static User newUser(String email) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$12$placeholder-bcrypt-hash")
                .fullName("Người dùng test")
                .build();
    }

}
