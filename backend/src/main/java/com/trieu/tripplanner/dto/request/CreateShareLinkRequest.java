package com.trieu.tripplanner.dto.request;

import java.time.Instant;

/**
 * Body of POST /api/v1/trips/{tripId}/share-links (design.md 10.2 "Quy ước Sharing API"). The whole body is
 * optional: no body, or no {@code expiresAt}, makes a link that never expires.
 * <p>
 * No Bean Validation annotation on purpose: the only rule, "in the future", needs the current time, and that
 * comes from the application's {@code Clock} in SharingService ({@code @Future} would read the system clock and
 * tie the tests to the day they run). A value that is not a timestamp at all is already a 400 when the body is
 * read. The permission of the link is not a field: the server decides it (VIEW).
 *
 * @param expiresAt the moment the link stops working, ISO-8601 in UTC ("2026-11-01T16:59:59Z"); null = never
 */
public record CreateShareLinkRequest(Instant expiresAt) {
}
