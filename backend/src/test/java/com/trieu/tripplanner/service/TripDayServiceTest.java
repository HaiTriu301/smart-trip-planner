package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.dto.request.UpdateTripDayRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.dto.response.TripDayDetailResponse;
import com.trieu.tripplanner.dto.response.TripDayResponse;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.ActivityMapperImpl;
import com.trieu.tripplanner.mapper.PlaceMapper;
import com.trieu.tripplanner.mapper.TripDayMapper;
import com.trieu.tripplanner.model.Activity;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.repository.ActivityRepository;
import com.trieu.tripplanner.repository.TripDayRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.support.TestUsers;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure unit test: repositories are mocked, the MapStruct mapper is real.
 */
@ExtendWith(MockitoExtension.class)
class TripDayServiceTest {

    private static final long TRIP_ID = 5L;
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private ActivityRepository activityRepository;

    private TripDayService tripDayService;

    @BeforeEach
    void setUp() {
        tripDayService = new TripDayService(tripDayRepository, tripRepository, activityRepository,
                Mappers.getMapper(TripDayMapper.class),
                new ActivityMapperImpl(Mappers.getMapper(PlaceMapper.class)));
    }

    @Nested
    class GenerateDays {

        @Test
        void generatesOneDayPerDateNumberedFromOne() {
            Trip trip = trip(OCT_1, OCT_1.plusDays(2));

            tripDayService.generateDays(trip);

            assertThat(savedDays())
                    .extracting(TripDay::getDayIndex, TripDay::getDate, TripDay::getTrip)
                    .containsExactly(
                            tuple(1, OCT_1, trip),
                            tuple(2, OCT_1.plusDays(1), trip),
                            tuple(3, OCT_1.plusDays(2), trip));
        }

        @Test
        void singleDayTripGetsExactlyOneDay() {
            tripDayService.generateDays(trip(OCT_1, OCT_1));

            assertThat(savedDays()).extracting(TripDay::getDayIndex, TripDay::getDate)
                    .containsExactly(tuple(1, OCT_1));
        }

        @Test
        void daysCrossMonthAndYearBoundaries() {
            LocalDate newYearsEve = LocalDate.of(2026, 12, 30);

            tripDayService.generateDays(trip(newYearsEve, newYearsEve.plusDays(3)));

            assertThat(savedDays()).extracting(TripDay::getDate).containsExactly(
                    LocalDate.of(2026, 12, 30), LocalDate.of(2026, 12, 31),
                    LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 2));
        }

        @Test
        void longestAllowedTripGetsSixtyDays() {
            tripDayService.generateDays(trip(OCT_1, OCT_1.plusDays(59)));

            List<TripDay> days = savedDays();
            assertThat(days).hasSize(60);
            assertThat(days.getLast().getDayIndex()).isEqualTo(60);
            assertThat(days.getLast().getDate()).isEqualTo(OCT_1.plusDays(59));
        }

        @SuppressWarnings("unchecked")
        private List<TripDay> savedDays() {
            ArgumentCaptor<List<TripDay>> captor = ArgumentCaptor.forClass(List.class);
            verify(tripDayRepository).saveAll(captor.capture());
            return captor.getValue();
        }

    }

    @Nested
    class ListWithActivities {

        @Test
        void putsEveryActivityUnderItsDayAndKeepsTheOrderOfTheQuery() {
            Trip trip = trip(OCT_1, OCT_1.plusDays(2));
            TripDay one = day(11L, trip, 1, OCT_1, "Đến nơi");
            TripDay two = day(12L, trip, 2, OCT_1.plusDays(1), null);
            TripDay three = day(13L, trip, 3, OCT_1.plusDays(2), null);
            when(tripDayRepository.findByTripIdOrderByDate(TRIP_ID)).thenReturn(List.of(one, two, three));
            // As the query returns them: by order_index then id, days interleaved
            when(activityRepository.findByTripIdInDisplayOrder(TRIP_ID)).thenReturn(List.of(
                    activity(21L, one, "Ăn sáng", 1000),
                    activity(31L, three, "Chợ đêm", 1000),
                    activity(22L, one, "Tham quan", 2000)));

            List<TripDayDetailResponse> days = tripDayService.listWithActivities(TRIP_ID);

            assertThat(days)
                    .extracting(TripDayDetailResponse::id, TripDayDetailResponse::dayIndex,
                            TripDayDetailResponse::date, TripDayDetailResponse::title)
                    .containsExactly(
                            tuple(11L, 1, OCT_1, "Đến nơi"),
                            tuple(12L, 2, OCT_1.plusDays(1), null),
                            tuple(13L, 3, OCT_1.plusDays(2), null));
            assertThat(days.get(0).activities())
                    .extracting(ActivityResponse::id, ActivityResponse::title, ActivityResponse::dayId)
                    .containsExactly(tuple(21L, "Ăn sáng", 11L), tuple(22L, "Tham quan", 11L));
            // A day without activities still appears, with an empty list rather than null
            assertThat(days.get(1).activities()).isEmpty();
            assertThat(days.get(2).activities()).extracting(ActivityResponse::title).containsExactly("Chợ đêm");
        }

        @Test
        void tripWithoutActivitiesGivesEveryDayAnEmptyList() {
            Trip trip = trip(OCT_1, OCT_1.plusDays(1));
            when(tripDayRepository.findByTripIdOrderByDate(TRIP_ID)).thenReturn(List.of(
                    day(11L, trip, 1, OCT_1, null),
                    day(12L, trip, 2, OCT_1.plusDays(1), null)));

            List<TripDayDetailResponse> days = tripDayService.listWithActivities(TRIP_ID);

            assertThat(days).hasSize(2).allSatisfy(day -> assertThat(day.activities()).isEmpty());
        }

        @Test
        void doesNotLoadTheTripAgain() {
            // TripService.get has just loaded the trip; a second look-up would be a wasted query
            tripDayService.listWithActivities(TRIP_ID);

            verifyNoInteractions(tripRepository);
        }

        private static Activity activity(long id, TripDay day, String title, int orderIndex) {
            Activity activity = Activity.builder()
                    .tripDay(day)
                    .title(title)
                    .orderIndex(orderIndex)
                    .createdBy(TestUsers.verified(7L, "owner@example.com"))
                    .build();
            ReflectionTestUtils.setField(activity, "id", id);
            return activity;
        }

    }

    @Nested
    class ListDays {

        @Test
        void returnsDaysOfLiveTripInRepositoryOrder() {
            Trip trip = trip(OCT_1, OCT_1.plusDays(1));
            when(tripRepository.existsById(TRIP_ID)).thenReturn(true);
            when(tripDayRepository.findByTripIdOrderByDate(TRIP_ID)).thenReturn(List.of(
                    day(11L, trip, 1, OCT_1, "Đến nơi"),
                    day(12L, trip, 2, OCT_1.plusDays(1), null)));

            List<TripDayResponse> days = tripDayService.list(TRIP_ID);

            assertThat(days).containsExactly(
                    new TripDayResponse(11L, 1, OCT_1, "Đến nơi", null),
                    new TripDayResponse(12L, 2, OCT_1.plusDays(1), null, null));
        }

        @Test
        void missingOrDeletedTripIsNotFoundAndDaysAreNotQueried() {
            when(tripRepository.existsById(TRIP_ID)).thenReturn(false);

            assertThatThrownBy(() -> tripDayService.list(TRIP_ID)).isInstanceOf(ResourceNotFoundException.class);
            verify(tripDayRepository, never()).findByTripIdOrderByDate(any());
        }

    }

    @Nested
    class UpdateDay {

        private static final long DAY_ID = 11L;

        private TripDay stored;

        @BeforeEach
        void storedDay() {
            stored = day(DAY_ID, trip(OCT_1, OCT_1), 1, OCT_1, "Tiêu đề cũ");
            stored.setNote("Ghi chú cũ");
        }

        @Test
        void setsTrimmedValues() {
            liveTripWithDay();

            TripDayResponse response = tripDayService.update(TRIP_ID, DAY_ID,
                    new UpdateTripDayRequest("  Khám phá trung tâm  ", "  Chợ Đà Lạt  "));

            assertThat(stored.getTitle()).isEqualTo("Khám phá trung tâm");
            assertThat(stored.getNote()).isEqualTo("Chợ Đà Lạt");
            assertThat(response).isEqualTo(new TripDayResponse(DAY_ID, 1, OCT_1, "Khám phá trung tâm", "Chợ Đà Lạt"));
        }

        @Test
        void nullKeepsTheCurrentValue() {
            liveTripWithDay();

            tripDayService.update(TRIP_ID, DAY_ID, new UpdateTripDayRequest("Tiêu đề mới", null));

            assertThat(stored.getTitle()).isEqualTo("Tiêu đề mới");
            assertThat(stored.getNote()).isEqualTo("Ghi chú cũ");
        }

        @Test
        void emptyOrBlankClearsTheValue() {
            liveTripWithDay();

            tripDayService.update(TRIP_ID, DAY_ID, new UpdateTripDayRequest("", "   "));

            assertThat(stored.getTitle()).isNull();
            assertThat(stored.getNote()).isNull();
        }

        @Test
        void dayOfAnotherTripIsNotFound() {
            when(tripRepository.existsById(TRIP_ID)).thenReturn(true);
            when(tripDayRepository.findByIdAndTripId(DAY_ID, TRIP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tripDayService.update(TRIP_ID, DAY_ID, new UpdateTripDayRequest("X", null)))
                    .isInstanceOf(ResourceNotFoundException.class);
            assertThat(stored.getTitle()).isEqualTo("Tiêu đề cũ");
        }

        @Test
        void missingOrDeletedTripIsNotFoundAndTheDayIsNotLoaded() {
            when(tripRepository.existsById(TRIP_ID)).thenReturn(false);

            assertThatThrownBy(() -> tripDayService.update(TRIP_ID, DAY_ID, new UpdateTripDayRequest("X", null)))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(tripDayRepository, never()).findByIdAndTripId(any(), any());
        }

        private void liveTripWithDay() {
            when(tripRepository.existsById(TRIP_ID)).thenReturn(true);
            when(tripDayRepository.findByIdAndTripId(DAY_ID, TRIP_ID)).thenReturn(Optional.of(stored));
        }

    }

    private static Trip trip(LocalDate start, LocalDate end) {
        return Trip.builder()
                .owner(TestUsers.verified(7L, "owner@example.com"))
                .title("Chuyến đi")
                .slug("chuyen-di-abc123")
                .startDate(start)
                .endDate(end)
                .build();
    }

    /** BaseEntity has no id setter on purpose; tests set it the way Hibernate would. */
    private static TripDay day(long id, Trip trip, int dayIndex, LocalDate date, String title) {
        TripDay day = TripDay.builder().trip(trip).dayIndex(dayIndex).date(date).title(title).build();
        ReflectionTestUtils.setField(day, "id", id);
        return day;
    }

}
