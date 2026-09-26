package com.trieu.tripplanner.model.enums;

/**
 * Lifecycle of a trip (design.md 5.2). New trips start as DRAFT; the status is changed only through
 * the dedicated status endpoint, never by the general PATCH (design.md 10.2 "Quy ước Trip API").
 * ARCHIVED trips do not count toward the FREE quota (design.md 14.9).
 */
public enum TripStatus {
    DRAFT,
    PLANNED,
    ONGOING,
    COMPLETED,
    ARCHIVED
}
