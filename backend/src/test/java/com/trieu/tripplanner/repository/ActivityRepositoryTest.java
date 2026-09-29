package com.trieu.tripplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.Activity;
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
 * Real MySQL (Testcontainers): the queries are SQL only a real database can prove.
 * Each test runs in a rolled-back transaction.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ActivityRepositoryTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User owner;
    private TripDay dayOne;
    private TripDay dayTwo;

    @BeforeEach
    void createTripWithTwoDays() {
        owner = entityManager.persist(User.builder()
                .email("owner@example.com")
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName("Chủ chuyến đi")
                .build());
        Trip trip = entityManager.persist(Trip.builder()
                .owner(owner)
                .title("Đà Lạt")
                .slug("da-lat-abc123")
                .startDate(OCT_1)
                .endDate(OCT_1.plusDays(1))
                .build());
        dayOne = entityManager.persist(TripDay.builder().trip(trip).dayIndex(1).date(OCT_1).build());
        dayTwo = entityManager.persist(TripDay.builder().trip(trip).dayIndex(2).date(OCT_1.plusDays(1)).build());
        entityManager.flush();
    }

    @Test
    void maxOrderIndexIsZeroForADayWithoutActivities() {
        assertThat(activityRepository.findMaxOrderIndexByTripDayId(dayOne.getId())).isZero();
    }

    @Test
    void maxOrderIndexIsTheHighestOfThatDayOnly() {
        // Inserted out of order on purpose; the other day holds a higher index that must not leak in
        entityManager.persist(activity(dayOne, "Ăn trưa", 2000));
        entityManager.persist(activity(dayOne, "Ăn sáng", 1000));
        entityManager.persist(activity(dayOne, "Chợ đêm", 3000));
        entityManager.persist(activity(dayTwo, "Ngày khác", 9000));
        entityManager.flush();

        assertThat(activityRepository.findMaxOrderIndexByTripDayId(dayOne.getId())).isEqualTo(3000);
    }

    private Activity activity(TripDay day, String title, int orderIndex) {
        return Activity.builder().tripDay(day).title(title).orderIndex(orderIndex).createdBy(owner).build();
    }

}
