package com.trieu.tripplanner.common.constant;

/**
 * Names of the caches kept in Redis (design.md 8.1). A cache must be declared in CacheConfig with its lifetime
 * and the type it stores before a method may name it: an undeclared name fails instead of quietly getting a
 * cache without an end.
 */
public final class CacheNames {

    /** Answers of the map source to a place search. */
    public static final String PLACE_SEARCH = "place:search";

    private CacheNames() {
    }

}
