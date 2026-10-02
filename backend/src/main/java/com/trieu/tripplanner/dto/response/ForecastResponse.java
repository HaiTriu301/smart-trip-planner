package com.trieu.tripplanner.dto.response;

import com.trieu.tripplanner.provider.weather.dto.WeatherCondition;

/**
 * The weather of one day (design.md 10.2 "Quy ước Weather API"). The day itself is on the element that holds
 * this forecast.
 *
 * @param condition                one of seven values; the UI picks its icon from it
 * @param tempMin                  degrees Celsius, one decimal
 * @param tempMax                  degrees Celsius, one decimal
 * @param precipitationProbability chance of rain, 0 to 100 percent
 */
public record ForecastResponse(
        WeatherCondition condition,
        double tempMin,
        double tempMax,
        int precipitationProbability) {
}
