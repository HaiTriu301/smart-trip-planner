package com.trieu.tripplanner.model.enums;

/**
 * Whether the account may sign in. BLOCKED is set by an admin; soft delete is a separate concept (deleted_at).
 */
public enum UserStatus {
    ACTIVE,
    BLOCKED
}
