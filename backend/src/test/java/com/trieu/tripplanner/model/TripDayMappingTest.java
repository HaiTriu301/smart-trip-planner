package com.trieu.tripplanner.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.trieu.tripplanner.TestcontainersConfiguration;
import jakarta.persistence.PersistenceException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Checks TripDay.java against V6__create_trip_days.sql on a real MySQL (Testcontainers, Flyway, ddl-auto=validate),
 * plus the constraints the service will rely on: one day per (trip, date), cascade on hard delete.
 * Each test runs in a rolled-back transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripDayMappingTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Trip trip;

    @BeforeEach
    void createTrip() {
        User owner = entityManager.persist(User.builder()
                .email("owner@example.com")
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName("Chủ chuyến đi")
                .build());
        trip = entityManager.persistAndFlush(Trip.builder()
                .owner(owner)
                .title("Đà Lạt 3 ngày")
                .slug("da-lat-3-ngay-abc123")
                .startDate(OCT_1)
                .endDate(OCT_1.plusDays(2))
                .build());
    }

    @Test
    void readsEveryColumnBackFromMysql() {
        String longNote = "Chợ Đà Lạt, hồ Xuân Hương. ".repeat(20); // longer than VARCHAR(255): proves the TEXT column
        TripDay saved = entityManager.persistAndFlush(TripDay.builder()
                .trip(trip)
                .dayIndex(1)
                .date(OCT_1)
                .title("Khám phá trung tâm")
                .note(longNote)
                .build());
        entityManager.clear(); // force a real SELECT instead of returning the cached instance

        TripDay found = entityManager.find(TripDay.class, saved.getId());

        assertThat(found.getTrip().getId()).isEqualTo(trip.getId());
        assertThat(found.getDayIndex()).isEqualTo(1);
        assertThat(found.getDate()).isEqualTo(OCT_1);
        assertThat(found.getTitle()).isEqualTo("Khám phá trung tâm");
        assertThat(found.getNote()).isEqualTo(longNote);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void titleAndNoteAreOptional() {
        TripDay saved = entityManager.persistAndFlush(day(1, OCT_1));

        assertThat(saved.getTitle()).isNull();
        assertThat(saved.getNote()).isNull();
    }

    @Test
    void sameDateTwiceInOneTripIsRejectedByUniqueKey() {
        entityManager.persistAndFlush(day(1, OCT_1));

        assertThatThrownBy(() -> entityManager.persistAndFlush(day(2, OCT_1)))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("uk_trip_days_trip_date");
    }

    @Test
    void dayIndexBelowOneIsRejectedByCheckConstraint() {
        assertThatThrownBy(() -> entityManager.persistAndFlush(day(0, OCT_1)))
                .isInstanceOf(PersistenceException.class)
                .hasStackTraceContaining("chk_trip_days_day_index");
    }

    @Test
    void hardDeletingTheTripDeletesItsDays() {
        entityManager.persist(day(1, OCT_1));
        entityManager.persist(day(2, OCT_1.plusDays(1)));
        entityManager.flush();

        // What the Phase 8 scheduler will do after 30 days in the bin (rule 14.8)
        jdbcTemplate.update("DELETE FROM trips WHERE id = ?", trip.getId());

        assertThat(countDays()).isZero();
    }

    @Test
    void softDeletingTheTripKeepsItsDays() {
        entityManager.persist(day(1, OCT_1));
        entityManager.persist(day(2, OCT_1.plusDays(1)));
        entityManager.flush();

        // Like TripServiceImpl.delete: only the trip is loaded. With the days still managed, remove(trip) fails at
        // flush with TransientPropertyValueException even though @SQLDelete only stamps deleted_at.
        entityManager.clear();
        entityManager.remove(entityManager.find(Trip.class, trip.getId()));
        entityManager.flush();

        // Only deleted_at is stamped on the trip, so restoring it later brings the days back untouched
        assertThat(countDays()).isEqualTo(2);
    }

    private TripDay day(int dayIndex, LocalDate date) {
        return TripDay.builder().trip(trip).dayIndex(dayIndex).date(date).build();
    }

    private Integer countDays() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trip_days WHERE trip_id = ?", Integer.class,
                trip.getId());
    }

}
