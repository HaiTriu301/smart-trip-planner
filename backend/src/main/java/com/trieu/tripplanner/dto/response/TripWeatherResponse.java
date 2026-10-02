package com.trieu.tripplanner.dto.response;

import java.util.List;

/**
 * Weather of a trip (design.md 10.2 "Quy ước Weather API").
 *
 * @param days exactly one element per day of the trip, in calendar order, with or without a forecast
 */
public record TripWeatherResponse(List<TripWeatherDayResponse> days) {
}
