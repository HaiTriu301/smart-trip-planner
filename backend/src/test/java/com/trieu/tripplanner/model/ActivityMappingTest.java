package com.trieu.tripplanner.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.enums.ActivityType;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Checks Activity.java against V7__create_activities.sql on a real MySQL (Testcontainers, Flyway,
 * ddl-auto=validate), plus the constraints the service will rely on: the time and cost CHECKs, cascade from the
 * day, no cascade from the creator. ActivityRepository arrives with the create endpoint, so this test talks to
 * the EntityManager directly. Each test runs in a rolled-back transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ActivityMappingTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime TEN_THIRTY = LocalTime.of(10, 30);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User owner;
    private Trip trip;
    private TripDay day;

    @BeforeEach
    void createTripWithOneDay() {
        owner = entityManager.persist(user("owner@example.com"));
        trip = entityManager.persist(Trip.builder()
                .owner(owner)
                .title("Đà Lạt 3 ngày")
                .slug("da-lat-3-ngay-abc123")
                .startDate(OCT_1)
                .endDate(OCT_1)
                .build());
        day = entityManager.persistAndFlush(TripDay.builder().trip(trip).dayIndex(1).date(OCT_1).build());
    }

    @Test
    void persistAppliesDefaultsAndStartsVersionAtZero() {
        Activity saved = entityManager.persistAndFlush(minimal("Ăn sáng").build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getType()).isEqualTo(ActivityType.OTHER);
        assertThat(saved.getVersion()).isZero();
        // Everything else is optional: an activity can be jotted down with just a title
        assertThat(saved.getStartTime()).isNull();
        assertThat(saved.getEndTime()).isNull();
        assertThat(saved.getNote()).isNull();
        assertThat(saved.getCostAmount()).isNull();
        assertThat(saved.getCurrency()).isNull();
        assertThat(saved.getBookingUrl()).isNull();
    }

    @Test
    void readsEveryColumnBackFromMysql() {
        String longNote = "Đặt bàn trước, quán đông. ".repeat(20); // longer than VARCHAR(255): proves the TEXT column
        Activity saved = entityManager.persistAndFlush(minimal("Ăn trưa lẩu gà lá é")
                .type(ActivityType.FOOD)
                .startTime(NINE)
                .endTime(TEN_THIRTY)
                .orderIndex(2000)
                .note(longNote)
                .costAmount(new BigDecimal("350000.50"))
                .currency("VND")
                .bookingUrl("https://example.com/booking/123")
                .build());
        entityManager.clear(); // force a real SELECT instead of returning the cached instance

        Activity found = entityManager.find(Activity.class, saved.getId());

        assertThat(found.getTripDay().getId()).isEqualTo(day.getId());
        assertThat(found.getTitle()).isEqualTo("Ăn trưa lẩu gà lá é");
        assertThat(found.getType()).isEqualTo(ActivityType.FOOD);
        assertThat(found.getStartTime()).isEqualTo(NINE);
        assertThat(found.getEndTime()).isEqualTo(TEN_THIRTY);
        assertThat(found.getOrderIndex()).isEqualTo(2000);
        assertThat(found.getNote()).isEqualTo(longNote);
        assertThat(found.getCostAmount()).isEqualByComparingTo("350000.50");
        assertThat(found.getCurrency()).isEqualTo("VND");
        assertThat(found.getBookingUrl()).isEqualTo("https://example.com/booking/123");
        assertThat(found.getCreatedBy().getId()).isEqualTo(owner.getId());
    }

    @Test
    void timesAreStoredAsWrittenWithoutTimeZoneShift() {
        // hibernate.jdbc.time_zone=UTC must not move a wall-clock time: a shifted value would wrap around
        // midnight, break chk_activities_time_range and every overlap comparison done in SQL.
        // 03:00 is the telling case: shifted by a +07:00 JVM it becomes 20:00 of the previous day.
        Activity saved = entityManager.persistAndFlush(minimal("Săn mây")
                .startTime(LocalTime.of(3, 0))
                .endTime(LocalTime.of(8, 15))
                .build());

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT CAST(start_time AS CHAR) AS s, CAST(end_time AS CHAR) AS e FROM activities WHERE id = ?",
                saved.getId());

        assertThat(row.get("s")).isEqualTo("03:00:00");
        assertThat(row.get("e")).isEqualTo("08:15:00");
    }

    @Test
    void updateIncrementsVersion() {
        Activity activity = entityManager.persistAndFlush(minimal("Chợ đêm").build());

        activity.setTitle("Chợ đêm Đà Lạt");
        entityManager.flush();

        assertThat(activity.getVersion()).isEqualTo(1L);
    }

    @Test
    void movingToAnotherDayAndPositionDoesNotIncrementVersion() {
        TripDay dayTwo = entityManager.persistAndFlush(
                TripDay.builder().trip(trip).dayIndex(2).date(OCT_1.plusDays(1)).build());
        Activity activity = entityManager.persistAndFlush(minimal("Chợ đêm").build());

        activity.moveTo(dayTwo, 2500);
        entityManager.flush();
        entityManager.clear();

        // Position is not content (design.md 11.3): a drag and drop must not make an open edit form stale
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT trip_day_id, order_index, version FROM activities WHERE id = ?", activity.getId());
        assertThat(((Number) row.get("trip_day_id")).longValue()).isEqualTo(dayTwo.getId());
        assertThat(row.get("order_index")).isEqualTo(2500);
        assertThat(((Number) row.get("version")).longValue()).isZero();
    }

    @Test
    void editingContentAfterAMoveStillIncrementsVersion() {
        Activity activity = entityManager.persistAndFlush(minimal("Chợ đêm").build());
        activity.moveTo(day, 2500);
        entityManager.flush();

        activity.setTitle("Chợ đêm Đà Lạt");
        entityManager.flush();

        assertThat(activity.getVersion()).isEqualTo(1L);
    }

    @Test
    void endTimeNotAfterStartTimeIsRejectedByCheckConstraint() {
        assertThatThrownBy(() -> entityManager.persistAndFlush(
                minimal("Sai giờ").startTime(TEN_THIRTY).endTime(NINE).build()))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("chk_activities_time_range");
    }

    @Test
    void endTimeWithoutStartTimeIsRejectedByCheckConstraint() {
        assertThatThrownBy(() -> entityManager.persistAndFlush(minimal("Thiếu giờ bắt đầu").endTime(NINE).build()))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("chk_activities_time_range");
    }

    @Test
    void startTimeAloneIsAllowed() {
        Activity saved = entityManager.persistAndFlush(minimal("Xuất phát").startTime(NINE).build());

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void negativeCostIsRejectedByCheckConstraint() {
        assertThatThrownBy(() -> entityManager.persistAndFlush(
                minimal("Giá âm").costAmount(new BigDecimal("-1.00")).build()))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("chk_activities_cost");
    }

    @Test
    void deletingTheDayDeletesItsActivities() {
        entityManager.persist(minimal("Ăn sáng").build());
        entityManager.persist(minimal("Hồ Xuân Hương").orderIndex(2000).build());
        entityManager.flush();

        // What cutting a day with force=true does (rule 14.3)
        jdbcTemplate.update("DELETE FROM trip_days WHERE id = ?", day.getId());

        assertThat(countActivities()).isZero();
    }

    @Test
    void hardDeletingTheTripDeletesItsActivitiesThroughItsDays() {
        entityManager.persistAndFlush(minimal("Ăn sáng").build());

        // What the Phase 8 scheduler will do after 30 days in the bin (rule 14.8)
        jdbcTemplate.update("DELETE FROM trips WHERE id = ?", trip.getId());

        assertThat(countActivities()).isZero();
    }

    @Test
    void hardDeletingTheCreatorIsBlockedWhileTheirActivitiesExist() {
        User editor = entityManager.persist(user("editor@example.com"));
        entityManager.persistAndFlush(minimal("Do người khác thêm").createdBy(editor).build());

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", editor.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("fk_activities_created_by");
        assertThat(countActivities()).isEqualTo(1);
    }

    /** Only the NOT NULL columns; each test adds what it is about. */
    private Activity.ActivityBuilder minimal(String title) {
        return Activity.builder().tripDay(day).title(title).orderIndex(1000).createdBy(owner);
    }

    private static User user(String email) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName("Người dùng thử")
                .build();
    }

    private Integer countActivities() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM activities", Integer.class);
    }

}
