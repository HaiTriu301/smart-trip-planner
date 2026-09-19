package com.trieu.tripplanner.model.enums;

/**
 * Global role of an account (design.md 5.2 users.role). Trip-level roles live in TripMember, not here.
 * Names are stored as-is in the MySQL ENUM column, so renaming a constant requires a migration.
 */
public enum Role {
    USER,
    ADMIN
}
