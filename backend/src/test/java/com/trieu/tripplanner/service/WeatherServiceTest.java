package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
 * The weather source and the repositories are mocks; the MapStruct mapper is the real generated one.
 */
@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    private static final long TRIP_ID = 5L;
    private static final BigDecimal LAT = new BigDecimal("16.0678000");
    private static final BigDecimal LNG = new BigDecimal("108.2208000");
    private static final LocalDate OCT_5 = LocalDate.of(2026, 10, 5);
    private static final LocalDate OCT_6 = OCT_5.plusDays(1);
    private static final LocalDate OCT_7 = OCT_5.plusDays(2);

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private WeatherProvider weatherProvider;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(tripRepository, tripDayRepository, weatherProvider,
                Mappers.getMapper(WeatherMapper.class));
    }

    @Test
    void givesEveryDayOfTheTripTheForecastAtItsDestination() {
        Trip trip = threeDayTrip(LAT, LNG);
        when(weatherProvider.forecast(LAT, LNG, OCT_5, OCT_7)).thenReturn(List.of(
                new DailyForecast(OCT_5, WeatherCondition.CLEAR, 24.1, 31.5, 10),
                new DailyForecast(OCT_6, WeatherCondition.RAIN, 23.0, 27.4, 70),
                new DailyForecast(OCT_7, WeatherCondition.CLOUDY, 22.6, 29.0, 45)));

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID);

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
        // Latest day first, the middle day missing, and one day that is not part of the trip
        when(weatherProvider.forecast(LAT, LNG, OCT_5, OCT_7)).thenReturn(List.of(
                new DailyForecast(OCT_7, WeatherCondition.CLOUDY, 22.6, 29.0, 45),
                new DailyForecast(OCT_7.plusDays(1), WeatherCondition.THUNDERSTORM, 22.0, 26.0, 95),
                new DailyForecast(OCT_5, WeatherCondition.CLEAR, 24.1, 31.5, 10)));

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID);

        assertThat(response.days()).containsExactly(
                new TripWeatherDayResponse(11L, OCT_5, new ForecastResponse(WeatherCondition.CLEAR, 24.1, 31.5, 10)),
                new TripWeatherDayResponse(12L, OCT_6, null),
                new TripWeatherDayResponse(13L, OCT_7, new ForecastResponse(WeatherCondition.CLOUDY, 22.6, 29.0, 45)));
    }

    @Test
    void sourceThatRepeatsADayDoesNotBreakTheAnswer() {
        threeDayTrip(LAT, LNG);
        when(weatherProvider.forecast(LAT, LNG, OCT_5, OCT_7)).thenReturn(List.of(
                new DailyForecast(OCT_5, WeatherCondition.CLEAR, 24.1, 31.5, 10),
                new DailyForecast(OCT_5, WeatherCondition.RAIN, 23.0, 27.4, 70)));

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID);

        assertThat(response.days().getFirst().forecast())
                .isEqualTo(new ForecastResponse(WeatherCondition.CLEAR, 24.1, 31.5, 10));
    }

    @Test
    void tripWithoutDestinationCoordinatesKeepsItsDaysAndNeverAsksTheSource() {
        threeDayTrip(null, null);

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID);

        assertThat(response.status()).isEqualTo(TripWeatherStatus.NO_DESTINATION);
        assertThat(response.days()).containsExactly(
                new TripWeatherDayResponse(11L, OCT_5, null),
                new TripWeatherDayResponse(12L, OCT_6, null),
                new TripWeatherDayResponse(13L, OCT_7, null));
        verifyNoInteractions(weatherProvider);
    }

    @Test
    void tripHoldingOnlyHalfACoordinateHasNoDestination() {
        // The trip service refuses half a coordinate, but a row written another way must not crash the page
        threeDayTrip(LAT, null);

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID);

        assertThat(response.status()).isEqualTo(TripWeatherStatus.NO_DESTINATION);
        verifyNoInteractions(weatherProvider);
    }

    @Test
    void tripWithDestinationIsOkEvenWhenTheSourceHasNoForecastAtAll() {
        threeDayTrip(LAT, LNG);
        when(weatherProvider.forecast(LAT, LNG, OCT_5, OCT_7)).thenReturn(List.of());

        TripWeatherResponse response = weatherService.forTrip(TRIP_ID);

        // "No forecast yet" is not "no destination": the UI must not ask for a destination here
        assertThat(response.status()).isEqualTo(TripWeatherStatus.OK);
        assertThat(response.days()).extracting(TripWeatherDayResponse::forecast).containsOnlyNulls().hasSize(3);
    }

    @Test
    void missingOrDeletedTripIsNotFoundAndNothingElseIsRead() {
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> weatherService.forTrip(TRIP_ID)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(tripDayRepository, weatherProvider);
    }

    /** A trip from 05/10 to 07/10 with its three days (ids 11, 12, 13), as the repositories would return it. */
    private Trip threeDayTrip(BigDecimal lat, BigDecimal lng) {
        Trip trip = Trip.builder()
                .owner(TestUsers.verified(7L, "owner@example.com"))
                .title("Đà Nẵng 3 ngày")
                .slug("da-nang-3-ngay-abc123")
                .destinationLat(lat)
                .destinationLng(lng)
                .startDate(OCT_5)
                .endDate(OCT_7)
                .build();
        when(tripRepository.findById(TRIP_ID)).thenReturn(Optional.of(trip));
        when(tripDayRepository.findByTripIdOrderByDate(TRIP_ID)).thenReturn(List.of(
                day(11L, trip, 1, OCT_5), day(12L, trip, 2, OCT_6), day(13L, trip, 3, OCT_7)));
        return trip;
    }

    /** BaseEntity has no id setter on purpose; tests set it the way Hibernate would. */
    private static TripDay day(long id, Trip trip, int dayIndex, LocalDate date) {
        TripDay day = TripDay.builder().trip(trip).dayIndex(dayIndex).date(date).build();
        ReflectionTestUtils.setField(day, "id", id);
        return day;
    }

}
