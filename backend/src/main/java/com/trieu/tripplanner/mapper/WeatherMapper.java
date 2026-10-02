package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.response.ForecastResponse;
import com.trieu.tripplanner.provider.weather.dto.DailyForecast;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Provider forecast → DTO, generated at compile time, so the API never exposes a type of the provider layer.
 * unmappedTargetPolicy=ERROR: a new response field without a source fails the build.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WeatherMapper {

    /**
     * The date of the forecast is left out: the response carries it on the trip day.
     *
     * @return null for a null forecast, which is how "no forecast for that day" reaches the response
     */
    ForecastResponse toResponse(DailyForecast forecast);

}
