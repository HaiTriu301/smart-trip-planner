package com.trieu.tripplanner.model.enums;

/**
 * Where a place comes from (design.md 5.2 "places"). OSM (Task 3.8) is added together with the code that
 * produces it; the ENUM column of V9 already accepts it.
 */
public enum PlaceProvider {
    /** The bundled data set (mock/places.json). */
    MOCK,
    /** Typed in by a user; has no source and no external id, and is private to its creator (rule 14.19). */
    MANUAL
}
