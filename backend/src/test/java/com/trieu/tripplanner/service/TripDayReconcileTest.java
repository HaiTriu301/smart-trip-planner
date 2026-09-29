package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.FieldViolation;
import com.trieu.tripplanner.mapper.ActivityMapperImpl;
import com.trieu.tripplanner.mapper.TripDayMapperImpl;
import com.trieu.tripplanner.model.Activity;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.repository.TripDayRepository;
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
 * design.md rule 14.3 against a real MySQL: the point is MySQL's row-by-row UNIQUE (trip_id, date) check,
 * which a mock cannot reproduce. TripDayService runs with the real repositories; each test is rolled back.
 * <p>
 * Starting point of every test: a trip 01/10 → 03/10 with days A, B, C (created by generateDays).
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TripDayService.class, TripDayMapperImpl.class,
        ActivityMapperImpl.class})
class TripDayReconcileTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_3 = LocalDate.of(2026, 10, 3);

    @Autowired
    private TripDayService tripDayService;

    @Autowired
    private TripDayRepository tripDayRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User owner;
    private Trip trip;
    private List<Long> originalIds;

    @BeforeEach
    void tripOfThreeTitledDays() {
        owner = entityManager.persist(User.builder()
                .email("owner@example.com")
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName("Chủ chuyến đi")
                .build());
        trip = entityManager.persist(Trip.builder()
                .owner(owner)
                .title("Đà Lạt")
                .slug("da-lat-abc123")
                .startDate(OCT_1)
                .endDate(OCT_3)
                .build());
        tripDayService.generateDays(trip);
        entityManager.flush();

        List<TripDay> days = tripDayRepository.findByTripIdOrderByDate(trip.getId());
        days.get(0).setTitle("A");
        days.get(1).setTitle("B");
        days.get(2).setTitle("C");
        originalIds = days.stream().map(TripDay::getId).toList();
        entityManager.flush();
        // Like TripServiceImpl.update: only the trip is managed when reconcile runs, no day is loaded yet
        entityManager.clear();
        trip = entityManager.find(Trip.class, trip.getId());
    }

    // ---- same number of days → the whole block moves ------------------------------------------------------------

    @Test
    void shiftOneDayLaterKeepsEveryDayWithoutUniqueKeyClash() {
        changeDates(OCT_1.plusDays(1), OCT_3.plusDays(1));

        assertThat(days()).extracting(TripDay::getDayIndex, TripDay::getDate, TripDay::getTitle)
                .containsExactly(
                        tuple(1, LocalDate.of(2026, 10, 2), "A"),
                        tuple(2, LocalDate.of(2026, 10, 3), "B"),
                        tuple(3, LocalDate.of(2026, 10, 4), "C"));
        // Same rows moved, not deleted and recreated: activities attached to them (Task 2.3) follow
        assertThat(days()).extracting(TripDay::getId).containsExactlyElementsOf(originalIds);
    }

    @Test
    void shiftOneDayEarlierKeepsEveryDayWithoutUniqueKeyClash() {
        changeDates(OCT_1.minusDays(1), OCT_3.minusDays(1));

        assertThat(days()).extracting(TripDay::getDayIndex, TripDay::getDate, TripDay::getTitle)
                .containsExactly(
                        tuple(1, LocalDate.of(2026, 9, 30), "A"),
                        tuple(2, LocalDate.of(2026, 10, 1), "B"),
                        tuple(3, LocalDate.of(2026, 10, 2), "C"));
        assertThat(days()).extracting(TripDay::getId).containsExactlyElementsOf(originalIds);
    }

    @Test
    void shiftAWeekLaterMovesTheBlockEvenWithoutOverlap() {
        changeDates(OCT_1.plusDays(7), OCT_3.plusDays(7));

        assertThat(days()).extracting(TripDay::getDate, TripDay::getTitle)
                .containsExactly(
                        tuple(LocalDate.of(2026, 10, 8), "A"),
                        tuple(LocalDate.of(2026, 10, 9), "B"),
                        tuple(LocalDate.of(2026, 10, 10), "C"));
    }

    // ---- different number of days → keep by calendar date ------------------------------------------------------

    @Test
    void extendingTheEndAddsEmptyDaysAfter() {
        changeDates(OCT_1, OCT_3.plusDays(2));

        assertThat(days()).extracting(TripDay::getDayIndex, TripDay::getDate, TripDay::getTitle)
                .containsExactly(
                        tuple(1, OCT_1, "A"),
                        tuple(2, LocalDate.of(2026, 10, 2), "B"),
                        tuple(3, OCT_3, "C"),
                        tuple(4, LocalDate.of(2026, 10, 4), null),
                        tuple(5, LocalDate.of(2026, 10, 5), null));
    }

    @Test
    void extendingTheStartAddsDaysBeforeAndRenumbers() {
        changeDates(OCT_1.minusDays(2), OCT_3);

        assertThat(days()).extracting(TripDay::getDayIndex, TripDay::getDate, TripDay::getTitle)
                .containsExactly(
                        tuple(1, LocalDate.of(2026, 9, 29), null),
                        tuple(2, LocalDate.of(2026, 9, 30), null),
                        tuple(3, OCT_1, "A"),
                        tuple(4, LocalDate.of(2026, 10, 2), "B"),
                        tuple(5, OCT_3, "C"));
    }

    @Test
    void cuttingTheEndDeletesTheLastDays() {
        changeDates(OCT_1, OCT_1);

        assertThat(days()).extracting(TripDay::getDayIndex, TripDay::getDate, TripDay::getTitle)
                .containsExactly(tuple(1, OCT_1, "A"));
    }

    @Test
    void cuttingTheStartDeletesTheFirstDaysAndRenumbers() {
        changeDates(OCT_3, OCT_3);

        // C stays on 03/10 (calendar date kept) and becomes "Ngày 1"
        assertThat(days()).extracting(TripDay::getDayIndex, TripDay::getDate, TripDay::getTitle)
                .containsExactly(tuple(1, OCT_3, "C"));
    }

    @Test
    void shiftingAndResizingFallsBackToCalendarDates() {
        // No date in common: backend does not guess, the UI asks for two steps (shift, then resize)
        changeDates(OCT_1.plusDays(7), OCT_3.plusDays(9));

        assertThat(days()).extracting(TripDay::getDayIndex, TripDay::getDate, TripDay::getTitle)
                .containsExactly(
                        tuple(1, LocalDate.of(2026, 10, 8), null),
                        tuple(2, LocalDate.of(2026, 10, 9), null),
                        tuple(3, LocalDate.of(2026, 10, 10), null),
                        tuple(4, LocalDate.of(2026, 10, 11), null),
                        tuple(5, LocalDate.of(2026, 10, 12), null));
    }

    // ---- rule 14.3: a cut day that holds activities blocks the change unless force = true ----------------------

    @Test
    void cuttingADayThatHoldsActivitiesIsBlockedAndNothingIsDeleted() {
        addActivity(0, "Ăn sáng");   // day A, kept
        addActivity(2, "Chợ đêm");   // day C, cut
        addActivity(2, "Ăn tối");

        assertThatThrownBy(() -> changeDates(OCT_1, OCT_1.plusDays(1)))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TRIP_DAY_HAS_ACTIVITIES);
                    // "2 activities in 1 day": what the client shows before asking for force=true
                    assertThat(ex.getDetails()).containsExactly(
                            FieldViolation.of("force", "error.trip.dropped-activities", "2", "1"));
                });

        assertThat(activitiesByDate()).containsExactly(
                "2026-10-01=Ăn sáng", "2026-10-03=Chợ đêm", "2026-10-03=Ăn tối");
        assertThat(countDays()).isEqualTo(3);
    }

    @Test
    void cuttingTheStartIsBlockedTheSameWay() {
        addActivity(0, "Ăn sáng");   // day A, cut

        assertThatThrownBy(() -> changeDates(OCT_1.plusDays(1), OCT_3))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TRIP_DAY_HAS_ACTIVITIES);
                    assertThat(ex.getDetails()).containsExactly(
                            FieldViolation.of("force", "error.trip.dropped-activities", "1", "1"));
                });

        assertThat(countDays()).isEqualTo(3);
    }

    @Test
    void forceCutsTheDayWithItsActivitiesAndKeepsTheOthers() {
        addActivity(0, "Ăn sáng");   // day A, kept
        addActivity(2, "Chợ đêm");   // day C, cut
        addActivity(2, "Ăn tối");

        changeDates(OCT_1, OCT_1.plusDays(1), true);

        assertThat(days()).extracting(TripDay::getDayIndex, TripDay::getTitle)
                .containsExactly(tuple(1, "A"), tuple(2, "B"));
        // Deleted by MySQL through fk_activities_trip_day ON DELETE CASCADE, not by the application
        assertThat(activitiesByDate()).containsExactly("2026-10-01=Ăn sáng");
    }

    @Test
    void cuttingAnEmptyDayIsNotBlockedEvenWhenOtherDaysHoldActivities() {
        addActivity(0, "Ăn sáng");
        addActivity(1, "Tham quan");

        changeDates(OCT_1, OCT_1.plusDays(1));   // cuts day C, which is empty

        assertThat(days()).extracting(TripDay::getTitle).containsExactly("A", "B");
        assertThat(activitiesByDate()).containsExactly("2026-10-01=Ăn sáng", "2026-10-02=Tham quan");
    }

    @Test
    void shiftingTheWholeTripNeverBlocksAndActivitiesFollowTheirDay() {
        addActivity(0, "Ăn sáng");
        addActivity(2, "Chợ đêm");

        changeDates(OCT_1.plusDays(7), OCT_3.plusDays(7));

        assertThat(activitiesByDate()).containsExactly("2026-10-08=Ăn sáng", "2026-10-10=Chợ đêm");
    }

    @Test
    void extendingTheTripNeverBlocks() {
        addActivity(2, "Chợ đêm");

        changeDates(OCT_1.minusDays(1), OCT_3.plusDays(1));

        assertThat(countDays()).isEqualTo(5);
        assertThat(activitiesByDate()).containsExactly("2026-10-03=Chợ đêm");
    }

    @Test
    void shiftingAndResizingIsBlockedWhenTheOldDaysHoldActivities() {
        addActivity(0, "Ăn sáng");
        addActivity(1, "Tham quan");
        addActivity(2, "Chợ đêm");

        // 08/10..12/10 shares no date with 01/10..03/10: every old day would be cut
        assertThatThrownBy(() -> changeDates(OCT_1.plusDays(7), OCT_3.plusDays(9)))
                .isInstanceOfSatisfying(BusinessRuleException.class, ex -> assertThat(ex.getDetails())
                        .containsExactly(FieldViolation.of("force", "error.trip.dropped-activities", "3", "3")));

        assertThat(countDays()).isEqualTo(3);
    }

    /** Persists an activity on day A (0), B (1) or C (2), then leaves only the trip managed, like the setup. */
    private void addActivity(int dayPosition, String title) {
        entityManager.persist(Activity.builder()
                .tripDay(entityManager.getEntityManager().getReference(TripDay.class, originalIds.get(dayPosition)))
                .title(title)
                .orderIndex(1000)
                .createdBy(entityManager.getEntityManager().getReference(User.class, owner.getId()))
                .build());
        entityManager.flush();
        entityManager.clear();
        trip = entityManager.find(Trip.class, trip.getId());
    }

    /** "date=title" per activity, read straight from MySQL in date then insertion order. */
    private List<String> activitiesByDate() {
        return jdbcTemplate.query("""
                SELECT CAST(d.date AS CHAR) AS day, a.title FROM activities a
                JOIN trip_days d ON d.id = a.trip_day_id
                WHERE d.trip_id = ? ORDER BY d.date, a.id""",
                (rs, row) -> rs.getString("day") + "=" + rs.getString("title"), trip.getId());
    }

    private Integer countDays() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trip_days WHERE trip_id = ?", Integer.class,
                trip.getId());
    }

    private void changeDates(LocalDate newStart, LocalDate newEnd) {
        changeDates(newStart, newEnd, false);
    }

    /** What TripServiceImpl.update does: new dates on the managed trip, then reconcile with the old ones. */
    private void changeDates(LocalDate newStart, LocalDate newEnd, boolean force) {
        LocalDate oldStart = trip.getStartDate();
        LocalDate oldEnd = trip.getEndDate();
        trip.setStartDate(newStart);
        trip.setEndDate(newEnd);
        tripDayService.reconcileDays(trip, oldStart, oldEnd, force);
        entityManager.flush();
        entityManager.clear(); // read back what MySQL holds, not the cached entities
    }

    private List<TripDay> days() {
        return tripDayRepository.findByTripIdOrderByDate(trip.getId());
    }

}
