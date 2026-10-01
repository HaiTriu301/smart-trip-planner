package com.trieu.tripplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.dto.internal.DroppedActivities;
import com.trieu.tripplanner.dto.internal.TripActivityCount;
import com.trieu.tripplanner.model.Activity;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.model.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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

    // ---- countInDaysOutsideRange (rule 14.3) ------------------------------------------------------------------

    @Test
    void countsActivitiesOfTheDaysOutsideTheRange() {
        TripDay dayThree = entityManager.persist(
                TripDay.builder().trip(dayOne.getTrip()).dayIndex(3).date(OCT_1.plusDays(2)).build());
        entityManager.persist(activity(dayOne, "Giữ lại", 1000));
        entityManager.persist(activity(dayTwo, "Mất 1", 1000));
        entityManager.persist(activity(dayTwo, "Mất 2", 2000));
        entityManager.persist(activity(dayThree, "Mất 3", 1000));
        entityManager.flush();
        Long tripId = dayOne.getTrip().getId();

        // keep 01/10 only → 02/10 and 03/10 are cut
        assertThat(activityRepository.countInDaysOutsideRange(tripId, OCT_1, OCT_1))
                .isEqualTo(new DroppedActivities(2, 3));
        // keep 02/10..03/10 → 01/10 is cut
        assertThat(activityRepository.countInDaysOutsideRange(tripId, OCT_1.plusDays(1), OCT_1.plusDays(2)))
                .isEqualTo(new DroppedActivities(1, 1));
        // the range covers every day → nothing is cut
        assertThat(activityRepository.countInDaysOutsideRange(tripId, OCT_1, OCT_1.plusDays(2)))
                .isEqualTo(new DroppedActivities(0, 0));
    }

    @Test
    void countIsZeroWhenTheCutDaysAreEmptyOrBelongToAnotherTrip() {
        Trip otherTrip = entityManager.persist(Trip.builder()
                .owner(owner)
                .title("Huế")
                .slug("hue-ghi789")
                .startDate(OCT_1)
                .endDate(OCT_1.plusDays(1))
                .build());
        TripDay otherDay = entityManager.persist(
                TripDay.builder().trip(otherTrip).dayIndex(2).date(OCT_1.plusDays(1)).build());
        entityManager.persist(activity(dayOne, "Giữ lại", 1000));
        entityManager.persist(activity(otherDay, "Chuyến khác", 1000));
        entityManager.flush();

        // 02/10 of this trip is cut and empty; the activity on 02/10 of the other trip must not be counted
        DroppedActivities dropped = activityRepository.countInDaysOutsideRange(dayOne.getTrip().getId(), OCT_1, OCT_1);

        assertThat(dropped).isEqualTo(new DroppedActivities(0, 0));
        assertThat(dropped.isEmpty()).isTrue();
    }

    // ---- countByTripIds (activity count on trip cards) ---------------------------------------------------------

    @Test
    void countsActivitiesPerTripOverAllDaysAndLeavesOutTripsWithoutActivities() {
        Trip withOne = entityManager.persist(Trip.builder()
                .owner(owner).title("Huế").slug("hue-ghi789").startDate(OCT_1).endDate(OCT_1).build());
        TripDay withOneDay = entityManager.persist(TripDay.builder().trip(withOne).dayIndex(1).date(OCT_1).build());
        Trip empty = entityManager.persist(Trip.builder()
                .owner(owner).title("Chưa lên lịch").slug("trong-jkl012").startDate(OCT_1).endDate(OCT_1).build());
        Trip notAsked = entityManager.persist(Trip.builder()
                .owner(owner).title("Không hỏi tới").slug("khac-mno345").startDate(OCT_1).endDate(OCT_1).build());
        TripDay notAskedDay = entityManager.persist(TripDay.builder().trip(notAsked).dayIndex(1).date(OCT_1).build());
        entityManager.persist(activity(dayOne, "Ăn sáng", 1000));
        entityManager.persist(activity(dayOne, "Ăn trưa", 2000));
        entityManager.persist(activity(dayTwo, "Chợ đêm", 1000));
        entityManager.persist(activity(withOneDay, "Đại Nội", 1000));
        entityManager.persist(activity(notAskedDay, "Không được đếm", 1000));
        entityManager.flush();
        Long threeActivities = dayOne.getTrip().getId();

        List<TripActivityCount> counts = activityRepository.countByTripIds(
                List.of(threeActivities, withOne.getId(), empty.getId()));

        // Both days of the first trip together; the empty trip has no row; the trip not asked for is not counted
        assertThat(counts).containsExactlyInAnyOrder(
                new TripActivityCount(threeActivities, 3),
                new TripActivityCount(withOne.getId(), 1));
    }

    // ---- findByTripIdInDisplayOrder (trip detail) -------------------------------------------------------------

    @Test
    void findByTripIdReturnsEveryActivityOfTheTripInDisplayOrderAndNothingElse() {
        Trip otherTrip = entityManager.persist(Trip.builder()
                .owner(owner)
                .title("Huế")
                .slug("hue-jkl012")
                .startDate(OCT_1)
                .endDate(OCT_1)
                .build());
        TripDay otherDay = entityManager.persist(
                TripDay.builder().trip(otherTrip).dayIndex(1).date(OCT_1).build());
        // Inserted out of order on purpose, days interleaved
        entityManager.persist(activity(dayTwo, "Ngày 2 - thứ hai", 2000));
        entityManager.persist(activity(dayOne, "Ngày 1 - thứ hai", 2000));
        entityManager.persist(activity(otherDay, "Chuyến khác", 500));
        entityManager.persist(activity(dayOne, "Ngày 1 - thứ nhất", 1000));
        entityManager.persist(activity(dayTwo, "Ngày 2 - thứ nhất", 1000));
        entityManager.flush();
        entityManager.clear();

        List<Activity> activities = activityRepository.findByTripIdInDisplayOrder(dayOne.getTrip().getId());

        // Grouped by day afterwards, so only the order inside each day matters
        assertThat(activities).filteredOn(a -> a.getTripDay().getId().equals(dayOne.getId()))
                .extracting(Activity::getTitle)
                .containsExactly("Ngày 1 - thứ nhất", "Ngày 1 - thứ hai");
        assertThat(activities).filteredOn(a -> a.getTripDay().getId().equals(dayTwo.getId()))
                .extracting(Activity::getTitle)
                .containsExactly("Ngày 2 - thứ nhất", "Ngày 2 - thứ hai");
        assertThat(activities).hasSize(4);
    }

    // ---- reorder queries ----------------------------------------------------------------------------------------

    @Test
    void findAllByIdInAndTripIdLeavesOutActivitiesOfOtherTripsAndUnknownIds() {
        Trip otherTrip = entityManager.persist(Trip.builder()
                .owner(owner)
                .title("Huế")
                .slug("hue-mno345")
                .startDate(OCT_1)
                .endDate(OCT_1)
                .build());
        TripDay otherDay = entityManager.persist(
                TripDay.builder().trip(otherTrip).dayIndex(1).date(OCT_1).build());
        Activity first = entityManager.persist(activity(dayOne, "Ăn sáng", 1000));
        Activity second = entityManager.persist(activity(dayTwo, "Chợ đêm", 1000));
        Activity foreign = entityManager.persist(activity(otherDay, "Chuyến khác", 1000));
        entityManager.flush();

        List<Activity> found = activityRepository.findAllByIdInAndTripId(
                List.of(first.getId(), second.getId(), foreign.getId(), 999_999L), dayOne.getTrip().getId());

        assertThat(found).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void findByTripDayIdInReturnsTheGivenDaysOnlyInDisplayOrder() {
        TripDay dayThree = entityManager.persist(
                TripDay.builder().trip(dayOne.getTrip()).dayIndex(3).date(OCT_1.plusDays(2)).build());
        entityManager.persist(activity(dayOne, "Ngày 1 - thứ hai", 2000));
        entityManager.persist(activity(dayThree, "Ngày 3", 500));
        entityManager.persist(activity(dayOne, "Ngày 1 - thứ nhất", 1000));
        entityManager.persist(activity(dayTwo, "Ngày 2", 1500));
        entityManager.flush();
        entityManager.clear();

        List<Activity> found = activityRepository.findByTripDayIdInDisplayOrder(
                List.of(dayOne.getId(), dayTwo.getId()));

        assertThat(found).extracting(Activity::getTitle)
                .containsExactly("Ngày 1 - thứ nhất", "Ngày 2", "Ngày 1 - thứ hai");
    }

    // ---- findByIdAndTripId ------------------------------------------------------------------------------------

    @Test
    void findByIdAndTripIdOnlyMatchesAnActivityOfThatTrip() {
        Trip otherTrip = entityManager.persist(Trip.builder()
                .owner(owner)
                .title("Huế")
                .slug("hue-def456")
                .startDate(OCT_1)
                .endDate(OCT_1)
                .build());
        Activity activity = entityManager.persist(activity(dayOne, "Ăn sáng", 1000));
        entityManager.flush();
        Long tripId = dayOne.getTrip().getId();

        assertThat(activityRepository.findByIdAndTripId(activity.getId(), tripId)).contains(activity);
        // Guessing an activity id through another trip's URL must not reach the activity
        assertThat(activityRepository.findByIdAndTripId(activity.getId(), otherTrip.getId())).isEmpty();
        assertThat(activityRepository.findByIdAndTripId(999_999L, tripId)).isEmpty();
    }

    // ---- findByTripDayIdOrderByOrderIndexAscIdAsc (display order) ---------------------------------------------

    @Test
    void listReturnsOnlyThatDayInDisplayOrder() {
        // Inserted out of order on purpose; times must not influence the order, only order_index does
        entityManager.persist(Activity.builder().tripDay(dayOne).title("Thứ ba").orderIndex(3000)
                .startTime(LocalTime.parse("07:00")).endTime(LocalTime.parse("08:00")).createdBy(owner).build());
        entityManager.persist(activity(dayOne, "Thứ nhất", 1000));
        entityManager.persist(activity(dayTwo, "Ngày khác", 500));
        entityManager.persist(activity(dayOne, "Thứ hai", 1500));
        entityManager.flush();
        entityManager.clear();

        assertThat(activityRepository.findByTripDayIdOrderByOrderIndexAscIdAsc(dayOne.getId()))
                .extracting(Activity::getTitle, Activity::getOrderIndex)
                .containsExactly(tuple("Thứ nhất", 1000), tuple("Thứ hai", 1500), tuple("Thứ ba", 3000));
    }

    @Test
    void listBreaksATieOnOrderIndexByAge() {
        // What two simultaneous inserts can produce: the same index twice
        entityManager.persist(activity(dayOne, "Tạo trước", 1000));
        entityManager.persist(activity(dayOne, "Tạo sau", 1000));
        entityManager.flush();
        entityManager.clear();

        assertThat(activityRepository.findByTripDayIdOrderByOrderIndexAscIdAsc(dayOne.getId()))
                .extracting(Activity::getTitle)
                .containsExactly("Tạo trước", "Tạo sau");
    }

    @Test
    void listIsEmptyForADayWithoutActivities() {
        assertThat(activityRepository.findByTripDayIdOrderByOrderIndexAscIdAsc(dayOne.getId())).isEmpty();
    }

    // ---- findTimedByTripDayId (candidates for the overlap check, rule 14.4) -----------------------------------

    @Test
    void timedReturnsOnlyActivitiesWithBothTimesOfThatDayEarliestFirst() {
        // Inserted out of order on purpose
        entityManager.persist(timed(dayOne, "Chiều", "14:00", "15:00"));
        entityManager.persist(timed(dayOne, "Sáng", "09:00", "10:00"));
        entityManager.persist(timed(dayOne, "Trưa", "11:30", "13:00"));
        entityManager.persist(activity(dayOne, "Chưa xếp giờ", 1000));
        entityManager.persist(Activity.builder().tripDay(dayOne).title("Chỉ có giờ bắt đầu").orderIndex(2000)
                .startTime(LocalTime.parse("09:30")).createdBy(owner).build());
        entityManager.persist(timed(dayTwo, "Ngày khác", "09:00", "10:00"));
        entityManager.flush();
        entityManager.clear();

        assertThat(activityRepository.findTimedByTripDayId(dayOne.getId()))
                .extracting(Activity::getTitle)
                .containsExactly("Sáng", "Trưa", "Chiều");
    }

    @Test
    void timedOfSeveralDaysReturnsOnlyTimedActivitiesOfThoseDays() {
        TripDay dayThree = entityManager.persist(
                TripDay.builder().trip(dayOne.getTrip()).dayIndex(3).date(OCT_1.plusDays(2)).build());
        entityManager.persist(timed(dayOne, "Ngày 1 - chiều", "14:00", "15:00"));
        entityManager.persist(timed(dayTwo, "Ngày 2 - sáng", "09:00", "10:00"));
        entityManager.persist(timed(dayThree, "Ngày 3", "08:00", "09:00"));
        entityManager.persist(activity(dayOne, "Chưa xếp giờ", 1000));
        entityManager.persist(timed(dayOne, "Ngày 1 - sáng sớm", "03:00", "04:00"));
        entityManager.flush();
        entityManager.clear();

        assertThat(activityRepository.findTimedByTripDayIdIn(List.of(dayOne.getId(), dayTwo.getId())))
                .extracting(Activity::getTitle, Activity::getStartTime)
                .containsExactly(
                        tuple("Ngày 1 - sáng sớm", LocalTime.of(3, 0)),
                        tuple("Ngày 2 - sáng", LocalTime.of(9, 0)),
                        tuple("Ngày 1 - chiều", LocalTime.of(14, 0)));
    }

    @Test
    void timedIsEmptyForADayWithoutTimedActivities() {
        entityManager.persist(activity(dayOne, "Chưa xếp giờ", 1000));
        entityManager.flush();

        assertThat(activityRepository.findTimedByTripDayId(dayOne.getId())).isEmpty();
    }

    @Test
    void timedReadsWallClockTimesBackUnshifted() {
        // The overlap check compares these values in Java, so they must be exactly what the user typed
        // (BUG-ACT-001, BUG-ACT-002): 03:00 must not come back as 20:00 on a +07:00 machine
        entityManager.persist(timed(dayOne, "Săn mây", "03:00", "04:00"));
        entityManager.flush();
        entityManager.clear();

        assertThat(activityRepository.findTimedByTripDayId(dayOne.getId()))
                .extracting(Activity::getStartTime, Activity::getEndTime)
                .containsExactly(tuple(LocalTime.of(3, 0), LocalTime.of(4, 0)));
    }

    private Activity timed(TripDay day, String title, String start, String end) {
        return Activity.builder().tripDay(day).title(title).orderIndex(1000)
                .startTime(LocalTime.parse(start)).endTime(LocalTime.parse(end)).createdBy(owner).build();
    }

    private Activity activity(TripDay day, String title, int orderIndex) {
        return Activity.builder().tripDay(day).title(title).orderIndex(orderIndex).createdBy(owner).build();
    }

}
