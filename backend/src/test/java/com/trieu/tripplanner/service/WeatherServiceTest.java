package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.dto.response.ForecastResponse;
import com.trieu.tripplanner.dto.response.TripWeatherDayResponse;
import com.trieu.tripplanner.dto.response.TripWeatherResponse;
import com.trieu.tripplanner.dto.response.TripWeatherStatus;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.WeatherMapper;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.provider.weather.WeatherProvider;
import com.trieu.tripplanner.provider.weather.dto.DailyForecast;
import com.trieu.tripplanner.provider.weather.dto.WeatherCondition;
import com.trieu.tripplanner.repository.TripDayRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.support.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The weather source, the repositories and "today" are mocks; the MapStruct mapper is the real generated one.
 */
@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    private static final long TRIP_ID = 5L;
    private static final long USER_ID = 7L;
    private static final BigDecimal LAT = new BigDecimal("16.0678000");
    private static final BigDecimal LNG = new BigDecimal("108.2208000");
    private static final LocalDate OCT_5 = LocalDate.of(2026, 10, 5);
    private static final LocalDate OCT_6 = OCT_5.plusDays(1);
    private static final LocalDate OCT_7 = OCT_5.plusDays(2);

    private static final ForecastResponse SOME_FORECAST = new ForecastResponse(WeatherCondition.CLEAR, 24.0, 30.0, 10);

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private WeatherProvider weatherProvider;

    @Mock
    private UserService userService;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(tripRepository, tripDayRepository, weatherProvider,
                Mappers.getMapper(WeatherMapper.class), userService);
    }

    @Test
    void givesEveryDayOfTheTripTheForecastAtItsDestination() {
        Trip trip = threeDayTrip(LAT, LNG);
        todayIs(OCT_5);
        when(weatherProvider.forecast(LAT, LNG, OCT_5, OCT_7)).thenReturn(List.of(
                new DailyForecast(OCT_5, WeatherCondition.CLEAR, 24.1, 31.5, 10),
                new DailyForecast(OCT_6, WeatherCondition.RAIN, 23.0, 27.4, 70),
                new DailyForecast(OCT_7, WeatherCondition.CLOUDY, 22.6, 29.0, 45)));

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID, USER_ID);

        assertThat(response.status()).isEqualTo(TripWeatherStatus.OK);
        assertThat(response.days()).containsExactly(
                new TripWeatherDayResponse(11L, OCT_5, new ForecastResponse(WeatherCondition.CLEAR, 24.1, 31.5, 10)),
                new TripWeatherDayResponse(12L, OCT_6, new ForecastResponse(WeatherCondition.RAIN, 23.0, 27.4, 70)),
                new TripWeatherDayResponse(13L, OCT_7, new ForecastResponse(WeatherCondition.CLOUDY, 22.6, 29.0, 45)));
        // The whole trip in one question, at the destination of the trip
        verify(weatherProvider).forecast(trip.getDestinationLat(), trip.getDestinationLng(), OCT_5, OCT_7);
    }

    @Test
    void matchesForecastsToDaysByDateNotByPosition() {
        threeDayTrip(LAT, LNG);
        todayIs(OCT_5);
        // Latest day first, the middle day missing, and one day that is not part of the trip
        when(weatherProvider.forecast(LAT, LNG, OCT_5, OCT_7)).thenReturn(List.of(
                new DailyForecast(OCT_7, WeatherCondition.CLOUDY, 22.6, 29.0, 45),
                new DailyForecast(OCT_7.plusDays(1), WeatherCondition.THUNDERSTORM, 22.0, 26.0, 95),
                new DailyForecast(OCT_5, WeatherCondition.CLEAR, 24.1, 31.5, 10)));

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID, USER_ID);

        assertThat(response.days()).containsExactly(
                new TripWeatherDayResponse(11L, OCT_5, new ForecastResponse(WeatherCondition.CLEAR, 24.1, 31.5, 10)),
                new TripWeatherDayResponse(12L, OCT_6, null),
                new TripWeatherDayResponse(13L, OCT_7, new ForecastResponse(WeatherCondition.CLOUDY, 22.6, 29.0, 45)));
    }

    @Test
    void sourceThatRepeatsADayDoesNotBreakTheAnswer() {
        threeDayTrip(LAT, LNG);
        todayIs(OCT_5);
        when(weatherProvider.forecast(LAT, LNG, OCT_5, OCT_7)).thenReturn(List.of(
                new DailyForecast(OCT_5, WeatherCondition.CLEAR, 24.1, 31.5, 10),
                new DailyForecast(OCT_5, WeatherCondition.RAIN, 23.0, 27.4, 70)));

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID, USER_ID);

        assertThat(response.days().getFirst().forecast())
                .isEqualTo(new ForecastResponse(WeatherCondition.CLEAR, 24.1, 31.5, 10));
    }

    @Test
    void tripWithoutDestinationCoordinatesKeepsItsDaysAndNeverAsksTheSource() {
        threeDayTrip(null, null);

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID, USER_ID);

        assertThat(response.status()).isEqualTo(TripWeatherStatus.NO_DESTINATION);
        assertThat(response.days()).containsExactly(
                new TripWeatherDayResponse(11L, OCT_5, null),
                new TripWeatherDayResponse(12L, OCT_6, null),
                new TripWeatherDayResponse(13L, OCT_7, null));
        // Not even "today" is looked up: there is nothing to compare it with
        verifyNoInteractions(weatherProvider, userService);
    }

    @Test
    void tripHoldingOnlyHalfACoordinateHasNoDestination() {
        // The trip service refuses half a coordinate, but a row written another way must not crash the page
        threeDayTrip(LAT, null);

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID, USER_ID);

        assertThat(response.status()).isEqualTo(TripWeatherStatus.NO_DESTINATION);
        verifyNoInteractions(weatherProvider);
    }

    @Test
    void tripWithDestinationIsOkEvenWhenTheSourceHasNoForecastAtAll() {
        threeDayTrip(LAT, LNG);
        todayIs(OCT_5);
        when(weatherProvider.forecast(LAT, LNG, OCT_5, OCT_7)).thenReturn(List.of());

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID, USER_ID);

        // "No forecast yet" is not "no destination": the UI must not ask for a destination here
        assertThat(response.status()).isEqualTo(TripWeatherStatus.OK);
        assertThat(response.days()).extracting(TripWeatherDayResponse::forecast).containsOnlyNulls().hasSize(3);
    }

    @Test
    void missingOrDeletedTripIsNotFoundAndNothingElseIsRead() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> weatherService.forTrip(TRIP_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(tripDayRepository, weatherProvider, userService);
    }

    @Test
    void daysAlreadyOverHaveNoForecastAndAreNotAskedFor() {
        threeDayTrip(LAT, LNG);
        todayIs(OCT_6);
        sourceAnswersEveryDayItIsAskedFor();

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID, USER_ID);

        assertThat(response.status()).isEqualTo(TripWeatherStatus.OK);
        assertThat(response.days()).containsExactly(
                new TripWeatherDayResponse(11L, OCT_5, null),
                new TripWeatherDayResponse(12L, OCT_6, SOME_FORECAST),   // today still has one
                new TripWeatherDayResponse(13L, OCT_7, SOME_FORECAST));
        verify(weatherProvider).forecast(LAT, LNG, OCT_6, OCT_7);
    }

    @Test
    void sixteenthDayFromTodayHasAForecastTheSeventeenthHasNone() {
        // Today is 05/10: the window is 05/10 .. 20/10. The trip runs 19/10 .. 22/10
        LocalDate oct19 = OCT_5.plusDays(14);
        LocalDate oct20 = OCT_5.plusDays(15);
        trip(oct19, oct19.plusDays(3), LAT, LNG);
        todayIs(OCT_5);
        sourceAnswersEveryDayItIsAskedFor();

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID, USER_ID);

        assertThat(response.days()).containsExactly(
                new TripWeatherDayResponse(11L, oct19, SOME_FORECAST),
                new TripWeatherDayResponse(12L, oct20, SOME_FORECAST),             // day 16, counting today
                new TripWeatherDayResponse(13L, oct20.plusDays(1), null),          // day 17
                new TripWeatherDayResponse(14L, oct20.plusDays(2), null));
        verify(weatherProvider).forecast(LAT, LNG, oct19, oct20);
    }

    @Test
    void tripThatIsOverNeverAsksTheSource() {
        threeDayTrip(LAT, LNG);
        todayIs(OCT_7.plusDays(1));

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID, USER_ID);

        // Still OK: the trip has a destination, there is just nothing to forecast any more
        assertThat(response.status()).isEqualTo(TripWeatherStatus.OK);
        assertThat(response.days()).extracting(TripWeatherDayResponse::forecast).containsOnlyNulls().hasSize(3);
        verifyNoInteractions(weatherProvider);
    }

    @Test
    void tripStartingOneDayBeyondTheWindowNeverAsksTheSource() {
        threeDayTrip(LAT, LNG);
        // 19/09 + 15 days = 04/10, the day before the trip starts
        todayIs(OCT_5.minusDays(WeatherService.FORECAST_DAYS));

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID, USER_ID);

        assertThat(response.days()).extracting(TripWeatherDayResponse::forecast).containsOnlyNulls().hasSize(3);
        verify(weatherProvider, never()).forecast(any(), any(), any(), any());
    }

    @Test
    void tripWhoseFirstDayIsTheLastDayOfTheWindowGetsThatOneForecast() {
        threeDayTrip(LAT, LNG);
        // 20/09 + 15 days = 05/10, the first day of the trip
        todayIs(OCT_5.minusDays(WeatherService.FORECAST_DAYS - 1));
        sourceAnswersEveryDayItIsAskedFor();

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID, USER_ID);

        assertThat(response.days()).containsExactly(
                new TripWeatherDayResponse(11L, OCT_5, SOME_FORECAST),
                new TripWeatherDayResponse(12L, OCT_6, null),
                new TripWeatherDayResponse(13L, OCT_7, null));
        verify(weatherProvider).forecast(LAT, LNG, OCT_5, OCT_5);
    }

    @Test
    void sourceAnsweringMoreThanItWasAskedDoesNotWidenTheWindow() {
        threeDayTrip(LAT, LNG);
        todayIs(OCT_6);
        // Asked for 06/10 .. 07/10, answers for yesterday as well
        when(weatherProvider.forecast(LAT, LNG, OCT_6, OCT_7)).thenReturn(List.of(
                new DailyForecast(OCT_5, WeatherCondition.CLEAR, 24.0, 30.0, 10),
                new DailyForecast(OCT_6, WeatherCondition.CLEAR, 24.0, 30.0, 10),
                new DailyForecast(OCT_7, WeatherCondition.CLEAR, 24.0, 30.0, 10)));

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID, USER_ID);

        assertThat(response.days()).extracting(TripWeatherDayResponse::forecast)
                .containsExactly(null, SOME_FORECAST, SOME_FORECAST);
    }

    private void todayIs(LocalDate today) {
        when(userService.today(USER_ID)).thenReturn(today);
    }

    /** Like the mock source of the application: one forecast for each day of the range, whatever the range. */
    private void sourceAnswersEveryDayItIsAskedFor() {
        when(weatherProvider.forecast(any(), any(), any(), any())).thenAnswer(call -> {
            LocalDate from = call.getArgument(2);
            LocalDate to = call.getArgument(3);
            return from.datesUntil(to.plusDays(1))
                    .map(date -> new DailyForecast(date, WeatherCondition.CLEAR, 24.0, 30.0, 10))
                    .toList();
        });
    }

    /** A trip from 05/10 to 07/10 with its three days (ids 11, 12, 13), as the repositories would return it. */
    private Trip threeDayTrip(BigDecimal lat, BigDecimal lng) {
        return trip(OCT_5, OCT_7, lat, lng);
    }

    /** A trip with one day per date of its range; the days get the ids 11, 12, 13... */
    private Trip trip(LocalDate start, LocalDate end, BigDecimal lat, BigDecimal lng) {
        Trip trip = Trip.builder()
                .owner(TestUsers.verified(USER_ID, "owner@example.com"))
                .title("Đà Nẵng")
                .slug("da-nang-abc123")
                .destinationLat(lat)
                .destinationLng(lng)
                .startDate(start)
                .endDate(end)
                .build();
        List<TripDay> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            days.add(day(11L + days.size(), trip, days.size() + 1, date));
        }
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripDayRepository.findByTripIdOrderByDate(TRIP_ID)).thenReturn(days);
        return trip;
    }

    /** BaseEntity has no id setter on purpose; tests set it the way Hibernate would. */
    private static TripDay day(long id, Trip trip, int dayIndex, LocalDate date) {
        TripDay day = TripDay.builder().trip(trip).dayIndex(dayIndex).date(date).build();
        ReflectionTestUtils.setField(day, "id", id);
        return day;
    }

}
