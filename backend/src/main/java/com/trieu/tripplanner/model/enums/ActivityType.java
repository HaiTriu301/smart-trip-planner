package com.trieu.tripplanner.model.enums;

/**
 * Kind of activity (design.md 5.2 "activities"); the UI picks the icon and colour from it.
 * Stored as a native MySQL ENUM: adding or renaming a constant needs a migration that ALTERs
 * activities.type (CLAUDE.md rule 10), ddl-auto=validate does not catch the difference.
 */
public enum ActivityType {
    SIGHTSEEING,
    FOOD,
    TRANSPORT,
    ACCOMMODATION,
    SHOPPING,
    OTHER
}
