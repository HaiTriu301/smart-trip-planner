package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.mapper.TripDayMapperImpl;
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
import org.springframework.test.context.ActiveProfiles;

/**
 * design.md rule 14.3 against a real MySQL: the point is MySQL's row-by-row UNIQUE (trip_id, date) check,
 * which a mock cannot reproduce. TripDayService runs with the real repositories; each test is rolled back.
 * <p>
 * Starting point of every test: a trip 01/10 → 03/10 with days A, B, C (created by generateDays).
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TripDayService.class, TripDayMapperImpl.class})
class TripDayReconcileTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_3 = LocalDate.of(2026, 10, 3);

    @Autowired
    private TripDayService tripDayService;

    @Autowired
    private TripDayRepository tripDayRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Trip trip;
    private List<Long> originalIds;

    @BeforeEach
    void tripOfThreeTitledDays() {
        User owner = entityManager.persist(User.builder()
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

    /** What TripServiceImpl.update does: new dates on the managed trip, then reconcile with the old ones. */
    private void changeDates(LocalDate newStart, LocalDate newEnd) {
        LocalDate oldStart = trip.getStartDate();
        LocalDate oldEnd = trip.getEndDate();
        trip.setStartDate(newStart);
        trip.setEndDate(newEnd);
        tripDayService.reconcileDays(trip, oldStart, oldEnd);
        entityManager.flush();
        entityManager.clear(); // read back what MySQL holds, not the cached entities
    }

    private List<TripDay> days() {
        return tripDayRepository.findByTripIdOrderByDate(trip.getId());
    }

}
