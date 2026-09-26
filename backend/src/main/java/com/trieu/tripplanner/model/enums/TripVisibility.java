package com.trieu.tripplanner.model.enums;

/**
 * Who may see a trip without being a member (design.md 5.2). Only PRIVATE has an effect until Phase 4
 * adds share links (LINK) and the public page (PUBLIC).
 */
public enum TripVisibility {
    /** Owner and invited members only. */
    PRIVATE,
    /** Anyone holding a share link. */
    LINK,
    /** Anyone, through the slug URL. */
    PUBLIC
}
