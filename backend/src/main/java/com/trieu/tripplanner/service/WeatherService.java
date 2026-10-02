package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.response.TripWeatherDayResponse;
import com.trieu.tripplanner.dto.response.TripWeatherResponse;
import com.trieu.tripplanner.dto.response.TripWeatherStatus;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.WeatherMapper;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.provider.weather.WeatherProvider;
import com.trieu.tripplanner.provider.weather.dto.DailyForecast;
import com.trieu.tripplanner.repository.TripDayRepository;
import com.trieu.tripplanner.repository.TripRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Weather of a trip (design.md 10.2 "Weather", rule 14.20). Talks to the weather source only through
 * {@link WeatherProvider}: switching from made-up numbers to a real service changes configuration, not this class
 * (CLAUDE.md rule 19). Simple enough to be a class without interface (CLAUDE.md rule 5).
 */
@Service
@RequiredArgsConstructor
public class WeatherService {

    /** A forecast exists for today and the fifteen days after it (design.md rule 14.20). */
    static final int FORECAST_DAYS = 16;

    private static final String TRIP = "Trip";

    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final WeatherProvider weatherProvider;
    private final WeatherMapper weatherMapper;
    private final UserService userService;

    /**
     * One element per day of the trip, in calendar order, each with the forecast at the destination of the trip
     * (one place for the whole trip, rule 14.20). A trip that has no destination coordinates yet gets its days
     * without any forecast and the status NO_DESTINATION, and the source is not asked. That is an answer, not an
     * error: a trip may be created first and given a destination later.
     * <p>
     * Only the next sixteen days have a forecast, today included; "today" is the calendar day of the caller
     * (rule 14.22). A day of the trip that is already over, or further away, keeps a null forecast. The rule
     * lives here and not in the source, so it holds for the mock source as well as for a real one.
     * <p>
     * Not @Transactional on purpose. Each repository call is one short read; a surrounding transaction would
     * keep a database connection busy for as long as the weather source takes to answer, and a real source is a
     * network call.
     *
     * @param userId the signed-in user, from the security context: whose "today" counts
     * @throws ResourceNotFoundException the trip does not exist or is deleted (404)
     */
    public TripWeatherResponse forTrip(Long tripId, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP, tripId));
        List<TripDay> days = tripDayRepository.findByTripIdOrderByDate(tripId);
        if (!hasDestination(trip)) {
            return new TripWeatherResponse(TripWeatherStatus.NO_DESTINATION, withForecasts(days, Map.of()));
        }
        LocalDate today = userService.today(userId);
        return new TripWeatherResponse(TripWeatherStatus.OK, withForecasts(days, forecastsByDate(trip, today)));
    }

    /** A point needs both numbers; a trip holding only one of them has no usable destination. */
    private static boolean hasDestination(Trip trip) {
        return trip.getDestinationLat() != null && trip.getDestinationLng() != null;
    }

    private List<TripWeatherDayResponse> withForecasts(List<TripDay> days, Map<LocalDate, DailyForecast> forecasts) {
        return days.stream()
                .map(day -> new TripWeatherDayResponse(day.getId(), day.getDate(),
                        weatherMapper.toResponse(forecasts.get(day.getDate()))))
                .toList();
    }

    /**
     * Forecasts keyed by their date: the source may skip days and promises no position, so a day of the trip
     * finds its forecast by date or finds nothing.
     * <p>
     * The source is asked only for the days the trip shares with the forecast window, and not at all when they
     * share none (a trip that is over, or one that starts more than sixteen days from now).
     */
    private Map<LocalDate, DailyForecast> forecastsByDate(Trip trip, LocalDate today) {
        LocalDate lastForecastDay = today.plusDays(FORECAST_DAYS - 1);
        LocalDate from = trip.getStartDate().isBefore(today) ? today : trip.getStartDate();
        LocalDate to = trip.getEndDate().isAfter(lastForecastDay) ? lastForecastDay : trip.getEndDate();
        if (from.isAfter(to)) {
            return Map.of();
        }
        return weatherProvider
                .forecast(trip.getDestinationLat(), trip.getDestinationLng(), from, to)
                .stream()
                // A source that answers more than it was asked must not widen the window
                .filter(forecast -> !forecast.date().isBefore(from) && !forecast.date().isAfter(to))
                // A source that repeats a day must not break the page: the first forecast of the day is kept
                .collect(Collectors.toMap(DailyForecast::date, Function.identity(), (first, repeated) -> first));
    }

}
