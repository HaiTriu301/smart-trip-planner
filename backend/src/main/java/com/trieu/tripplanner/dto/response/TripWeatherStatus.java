package com.trieu.tripplanner.dto.response;

/**
 * Why a trip has, or has not, a forecast (design.md 10.2 "Quy ước Weather API"). Part of the response only:
 * nothing of it is stored, so it lives next to the response and not in model/enums.
 */
public enum TripWeatherStatus {

    /** The trip has a destination; each day carries its forecast, or none when there is none for that day. */
    OK,

    /** The trip has no destination coordinates yet: no day has a forecast and the UI asks for a destination. */
    NO_DESTINATION,

    /**
     * The weather source did not answer (down, too slow, unreadable): no day has a forecast this time. The trip
     * page must stay usable without weather, so this is an answer and not an error; the next call asks again.
     */
    UNAVAILABLE
}
