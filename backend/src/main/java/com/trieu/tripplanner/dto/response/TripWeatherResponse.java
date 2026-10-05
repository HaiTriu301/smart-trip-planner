package com.trieu.tripplanner.dto.response;

import java.util.List;

/**
 * Weather of a trip (design.md 10.2 "Quy ước Weather API").
 *
 * @param status tells "no forecast because the trip has no destination" and "the weather source did not answer"
 *               apart from "no forecast for that day"
 * @param days   exactly one element per day of the trip, in calendar order, with or without a forecast
 */
public record TripWeatherResponse(
        TripWeatherStatus status,
        List<TripWeatherDayResponse> days) {
}
