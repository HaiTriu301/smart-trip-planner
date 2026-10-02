package com.trieu.tripplanner.provider.weather.dto;

import java.time.LocalDate;

/**
 * The forecast of one calendar day at one point, as a weather source describes it.
 *
 * @param date                     the day the forecast is for, in the local calendar of that point
 * @param tempMin                  lowest temperature of the day, degrees Celsius, one decimal
 * @param tempMax                  highest temperature of the day, degrees Celsius, one decimal
 * @param precipitationProbability chance of rain during the day, 0 to 100 percent
 */
public record DailyForecast(
        LocalDate date,
        WeatherCondition condition,
        double tempMin,
        double tempMax,
        int precipitationProbability) {
}
