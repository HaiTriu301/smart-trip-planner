package com.trieu.tripplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.model.User;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Real MySQL (Testcontainers): the derived queries are SQL only a real database can prove.
 * Each test runs in a rolled-back transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripDayRepositoryTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    @Autowired
    private TripDayRepository tripDayRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User owner;

    @BeforeEach
    void createOwner() {
        owner = entityManager.persist(User.builder()
                .email("owner@example.com")
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName("Chủ chuyến đi")
                .build());
    }

    @Test
    void findByTripIdOrderByDateReturnsOnlyThatTripSortedByDate() {
        Trip trip = trip("da-lat-aaaaaa");
        Trip other = trip("hue-bbbbbb");
        // Inserted out of order on purpose
        entityManager.persist(day(trip, 3, OCT_1.plusDays(2)));
        entityManager.persist(day(trip, 1, OCT_1));
        entityManager.persist(day(other, 1, OCT_1));
        entityManager.persist(day(trip, 2, OCT_1.plusDays(1)));
        entityManager.flush();
        entityManager.clear();

        assertThat(tripDayRepository.findByTripIdOrderByDate(trip.getId()))
                .extracting(TripDay::getDayIndex, TripDay::getDate)
                .containsExactly(
                        tuple(1, OCT_1),
                        tuple(2, OCT_1.plusDays(1)),
                        tuple(3, OCT_1.plusDays(2)));
    }

    @Test
    void findByTripIdOrderByDateIsEmptyForUnknownTrip() {
        assertThat(tripDayRepository.findByTripIdOrderByDate(999_999L)).isEmpty();
    }

    @Test
    void findByIdAndTripIdOnlyMatchesTheDayOfThatTrip() {
        Trip trip = trip("da-lat-cccccc");
        Trip other = trip("hue-dddddd");
        TripDay day = entityManager.persist(day(trip, 1, OCT_1));
        entityManager.flush();

        assertThat(tripDayRepository.findByIdAndTripId(day.getId(), trip.getId())).contains(day);
        // Guessing a day id through another trip's URL must not reach the day
        assertThat(tripDayRepository.findByIdAndTripId(day.getId(), other.getId())).isEmpty();
    }

    private Trip trip(String slug) {
        return entityManager.persist(Trip.builder()
                .owner(owner)
                .title("Chuyến đi")
                .slug(slug)
                .startDate(OCT_1)
                .endDate(OCT_1.plusDays(2))
                .build());
    }

    private static TripDay day(Trip trip, int dayIndex, LocalDate date) {
        return TripDay.builder().trip(trip).dayIndex(dayIndex).date(date).build();
    }

}
