package com.trieu.tripplanner.common.constant;

/**
 * Names of the caches kept in Redis (design.md 8.1). A cache must be declared in CacheConfig with its lifetime
 * and the type it stores before a method may name it: an undeclared name fails instead of quietly getting a
 * cache without an end.
 */
public final class CacheNames {

    /** Answers of the map source to a place search. */
    public static final String PLACE_SEARCH = "place:search";

    /** Answers of the map source for the travel through one list of points. */
    public static final String ROUTE_LEGS = "route:legs";

    /** Answers of the weather source for one point and one range of days. */
    public static final String WEATHER_FORECAST = "weather:forecast";

    private CacheNames() {
    }

}
