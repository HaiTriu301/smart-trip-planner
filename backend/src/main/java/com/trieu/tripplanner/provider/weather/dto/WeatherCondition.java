package com.trieu.tripplanner.provider.weather.dto;

/**
 * What the sky does on one day (design.md 10.2 "Quy ước Weather API"). This list is part of the API: the UI picks
 * one icon per value, so every value a real source can report is declared from the start, even those the mock
 * source never produces (FOG, SNOW). Not stored in the database: adding a value needs no migration.
 * <p>
 * A real source maps its own codes onto these seven; drizzle and rain showers both count as RAIN.
 */
public enum WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    RAIN,
    THUNDERSTORM,
    SNOW
}
