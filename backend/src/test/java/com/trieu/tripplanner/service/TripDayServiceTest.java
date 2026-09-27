package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.dto.response.TripDayResponse;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.TripDayMapper;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.repository.TripDayRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.support.TestUsers;
import java.time.LocalDate;
import java.util.List;
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

    private TripDayService tripDayService;

    @BeforeEach
    void setUp() {
        tripDayService = new TripDayService(tripDayRepository, tripRepository, Mappers.getMapper(TripDayMapper.class));
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
