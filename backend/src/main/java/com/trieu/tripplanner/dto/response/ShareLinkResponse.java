package com.trieu.tripplanner.dto.response;

import com.trieu.tripplanner.model.enums.SharePermission;
import java.time.Instant;

/**
 * A share link as the owner of the trip sees it (design.md 10.2 "Quy ước Sharing API"). The owner is the only
 * one who ever receives this, so the token is in it: the client builds the address {@code {origin}/share/{token}}
 * from it and lets the owner copy it again later.
 *
 * @param permission always VIEW in Phase 4; set by the server, never taken from the client
 * @param expiresAt  the moment the link stops working; null = it never expires
 * @param viewCount  successful openings of the public page through this link
 * @param expired    true once {@code expiresAt} is no longer ahead of the server's clock: the link is still
 *                   listed, but it opens nothing. Worked out by the server so the client needs no clock of its own
 */
public record ShareLinkResponse(
        Long id,
        String token,
        SharePermission permission,
        Instant expiresAt,
        int viewCount,
        Instant createdAt,
        boolean expired) {
}
