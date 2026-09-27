package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;

import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.repository.TripDayRepository;
import com.trieu.tripplanner.support.TestUsers;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TripDayServiceTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    @Mock
    private TripDayRepository tripDayRepository;

    @InjectMocks
    private TripDayService tripDayService;

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

    private static Trip trip(LocalDate start, LocalDate end) {
        return Trip.builder()
                .owner(TestUsers.verified(7L, "owner@example.com"))
                .title("Chuyến đi")
                .slug("chuyen-di-abc123")
                .startDate(start)
                .endDate(end)
                .build();
    }

}
