package com.trieu.tripplanner.dto.response;

import java.time.LocalDate;

/**
 * One day of a trip with its weather.
 *
 * @param dayId    id of the trip day, so the UI can put the forecast next to the day it already shows
 * @param forecast null when there is no forecast for that day ("chưa có dự báo")
 */
public record TripWeatherDayResponse(
        Long dayId,
        LocalDate date,
        ForecastResponse forecast) {
}
