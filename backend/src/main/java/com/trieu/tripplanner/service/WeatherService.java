package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.response.TripWeatherDayResponse;
import com.trieu.tripplanner.dto.response.TripWeatherResponse;
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

    private static final String TRIP = "Trip";

    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final WeatherProvider weatherProvider;
    private final WeatherMapper weatherMapper;

    /**
     * One element per day of the trip, in calendar order, each with the forecast at the destination of the trip
     * (one place for the whole trip, rule 14.20). A trip that has no destination coordinates yet gets its days
     * without any forecast, and the source is not asked.
     * <p>
     * Not @Transactional on purpose. Each repository call is one short read; a surrounding transaction would
     * keep a database connection busy for as long as the weather source takes to answer, and a real source is a
     * network call.
     *
     * @throws ResourceNotFoundException the trip does not exist or is deleted (404)
     */
    public TripWeatherResponse forTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP, tripId));
        List<TripDay> days = tripDayRepository.findByTripIdOrderByDate(tripId);
        Map<LocalDate, DailyForecast> forecasts = forecastsByDate(trip);
        return new TripWeatherResponse(days.stream()
                .map(day -> new TripWeatherDayResponse(day.getId(), day.getDate(),
                        weatherMapper.toResponse(forecasts.get(day.getDate()))))
                .toList());
    }

    /**
     * Forecasts keyed by their date: the source may skip days and promises no position, so a day of the trip
     * finds its forecast by date or finds nothing.
     */
    private Map<LocalDate, DailyForecast> forecastsByDate(Trip trip) {
        if (trip.getDestinationLat() == null || trip.getDestinationLng() == null) {
            return Map.of();
        }
        return weatherProvider
                .forecast(trip.getDestinationLat(), trip.getDestinationLng(), trip.getStartDate(), trip.getEndDate())
                .stream()
                // A source that repeats a day must not break the page: the first forecast of the day is kept
                .collect(Collectors.toMap(DailyForecast::date, Function.identity(), (first, repeated) -> first));
    }

}
