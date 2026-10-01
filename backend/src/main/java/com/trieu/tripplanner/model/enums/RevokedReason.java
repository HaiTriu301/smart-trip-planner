package com.trieu.tripplanner.model.enums;

/**
 * Why a refresh token stopped being valid before its expiry (design.md 6.1). Must list exactly the values of
 * the ENUM column in V8__add_refresh_token_revoked_reason.sql.
 */
public enum RevokedReason {
    /** Exchanged for a new token by /auth/refresh. Seeing it again means someone kept a copy. */
    ROTATED,
    LOGOUT,
    /** Every session is closed when the password changes (rule 14.16). */
    PASSWORD_RESET,
    /** The account was blocked while the session was alive. */
    BLOCKED,
    /** Presented after its expiry. */
    EXPIRED,
    /** Closed together with every other session because a rotated token came back. */
    REUSE_DETECTED
}
