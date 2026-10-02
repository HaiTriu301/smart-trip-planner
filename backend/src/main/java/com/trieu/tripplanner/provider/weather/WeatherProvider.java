package com.trieu.tripplanner.provider.weather;

import com.trieu.tripplanner.provider.weather.dto.DailyForecast;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Port for the weather forecast (design.md 7.2). The implementation is chosen by {@code app.providers.weather}:
 * {@code mock} computes stable numbers without any network, {@code open-meteo} (Task 3.8) calls the real service.
 * Services depend on this interface only (CLAUDE.md rule 19).
 */
public interface WeatherProvider {

    /**
     * Daily forecast at one point for a range of days.
     *
     * @param lat  latitude of the point, decimal degrees
     * @param lng  longitude of the point, decimal degrees
     * @param from first day wanted
     * @param to   last day wanted, included; never before {@code from}
     * @return at most one forecast per day of the range, earliest first. A day the source has no forecast for is
     *         simply missing, so callers match the result by {@link DailyForecast#date()}, never by position.
     *         The same input gives the same list as long as the source has not published a newer forecast
     */
    List<DailyForecast> forecast(BigDecimal lat, BigDecimal lng, LocalDate from, LocalDate to);

}
