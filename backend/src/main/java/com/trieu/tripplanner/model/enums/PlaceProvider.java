package com.trieu.tripplanner.model.enums;

/**
 * Where a place comes from (design.md 5.2 "places"). The ENUM column of V9 accepts all three values.
 */
public enum PlaceProvider {
    /** The bundled data set (mock/places.json). */
    MOCK,
    /** OpenStreetMap, through the public search services (Task 3.8); the external id is like "W204885903". */
    OSM,
    /** Typed in by a user; has no source and no external id, and is private to its creator (rule 14.19). */
    MANUAL
}
