package com.trieu.tripplanner.model.enums;

/**
 * What the holder of a share link may do (design.md 5.2 "share_links", 6.2), as stored in
 * {@code share_links.permission}. Phase 4 only knows view links: adding EDIT here needs a migration that ALTERs
 * the ENUM column too (CLAUDE.md rule 10), and a way for somebody without an account to write.
 */
public enum SharePermission {
    /** Reads the trip through the public page; never writes. */
    VIEW
}
