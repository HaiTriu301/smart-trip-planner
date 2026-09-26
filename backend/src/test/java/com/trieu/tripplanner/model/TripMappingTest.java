package com.trieu.tripplanner.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.enums.TripStatus;
import com.trieu.tripplanner.model.enums.TripVisibility;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Checks Trip.java against V5__create_trips_table.sql on a real MySQL (Testcontainers, Flyway, ddl-auto=validate).
 * TripRepository only arrives with the CRUD milestone, so this test talks to the EntityManager directly.
 * Each test runs in a rolled-back transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripMappingTest {

    private static final LocalDate START = LocalDate.of(2026, 10, 1);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User owner;

    @BeforeEach
    void createOwner() {
        owner = entityManager.persistFlushFind(User.builder()
                .email("owner@example.com")
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName("Chủ chuyến đi")
                .build());
    }

    @Test
    void persistAppliesDefaultsAndStartsVersionAtZero() {
        Trip saved = entityManager.persistAndFlush(minimalTrip("da-lat-3-ngay-x7k2qp"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(TripStatus.DRAFT);
        assertThat(saved.getVisibility()).isEqualTo(TripVisibility.PRIVATE);
        assertThat(saved.getCurrency()).isEqualTo(Trip.DEFAULT_CURRENCY);
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    void readsEveryColumnBackFromMysql() {
        Trip saved = entityManager.persistAndFlush(Trip.builder()
                .owner(owner)
                .title("Đà Lạt mùa hoa")
                .slug("da-lat-mua-hoa-a1b2c3")
                .description("Lịch trình ".repeat(100)) // longer than VARCHAR(255): proves the TEXT column
                .coverImageUrl("https://example.com/cover.jpg")
                .destinationName("Đà Lạt, Lâm Đồng")
                .destinationLat(new BigDecimal("11.9404192"))
                .destinationLng(new BigDecimal("108.4583132"))
                .startDate(START)
                .endDate(START.plusDays(2))
                .budgetAmount(new BigDecimal("5500000.50"))
                .currency("USD")
                .status(TripStatus.PLANNED)
                .visibility(TripVisibility.PUBLIC)
                .build());
        entityManager.clear(); // force a real SELECT instead of returning the cached instance

        Trip found = entityManager.find(Trip.class, saved.getId());

        assertThat(found.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(found.getTitle()).isEqualTo("Đà Lạt mùa hoa");
        assertThat(found.getSlug()).isEqualTo("da-lat-mua-hoa-a1b2c3");
        assertThat(found.getDescription()).hasSize(1100);
        assertThat(found.getCoverImageUrl()).isEqualTo("https://example.com/cover.jpg");
        assertThat(found.getDestinationName()).isEqualTo("Đà Lạt, Lâm Đồng");
        assertThat(found.getDestinationLat()).isEqualByComparingTo("11.9404192");
        assertThat(found.getDestinationLng()).isEqualByComparingTo("108.4583132");
        assertThat(found.getStartDate()).isEqualTo(START);
        assertThat(found.getEndDate()).isEqualTo(START.plusDays(2));
        assertThat(found.getBudgetAmount()).isEqualByComparingTo("5500000.50");
        assertThat(found.getCurrency()).isEqualTo("USD");
        assertThat(found.getStatus()).isEqualTo(TripStatus.PLANNED);
        assertThat(found.getVisibility()).isEqualTo(TripVisibility.PUBLIC);
    }

    @Test
    void updateIncrementsVersion() {
        Trip trip = entityManager.persistAndFlush(minimalTrip("vung-tau-q1w2e3"));

        trip.setTitle("Vũng Tàu cuối tuần");
        entityManager.flush();

        assertThat(trip.getVersion()).isEqualTo(1L);
    }

    @Test
    void removeOnlyStampsDeletedAtAndHidesTheRow() {
        Trip trip = entityManager.persistAndFlush(minimalTrip("hue-r4t5y6"));
        Long id = trip.getId();

        entityManager.remove(trip);
        entityManager.flush();
        entityManager.clear();

        // The row is still there, only stamped (and versioned like any other write)
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT deleted_at, version FROM trips WHERE id = ?", id);
        assertThat(row.get("deleted_at")).isNotNull();
        assertThat(((Number) row.get("version")).longValue()).isEqualTo(1L);
        // ...but @SQLRestriction hides it from every JPA read
        assertThat(entityManager.find(Trip.class, id)).isNull();
    }

    @Test
    void duplicateSlugIsRejectedByUniqueKey() {
        entityManager.persistAndFlush(minimalTrip("sa-pa-u7i8o9"));

        assertThatThrownBy(() -> entityManager.persistAndFlush(minimalTrip("sa-pa-u7i8o9")))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("uk_trips_slug");
    }

    @Test
    void endDateBeforeStartDateIsRejectedByCheckConstraint() {
        Trip invalid = Trip.builder()
                .owner(owner)
                .title("Ngày ngược")
                .slug("ngay-nguoc-p0a1s2")
                .startDate(START)
                .endDate(START.minusDays(1))
                .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(invalid))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("chk_trips_date_range");
    }

    private Trip minimalTrip(String slug) {
        return Trip.builder()
                .owner(owner)
                .title("Chuyến đi thử")
                .slug(slug)
                .startDate(START)
                .endDate(START.plusDays(2))
                .build();
    }

}
