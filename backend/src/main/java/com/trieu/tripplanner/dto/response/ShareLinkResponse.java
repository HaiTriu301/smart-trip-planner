package com.trieu.tripplanner.dto.response;

import com.trieu.tripplanner.model.enums.SharePermission;
import java.time.Instant;

/**
 * A share link as the owner of the trip sees it (design.md 10.2 "Quy ước Sharing API"). The owner is the only
 * one who ever receives this, so the token is in it: the client builds the address {@code {origin}/share/{token}}
 * from it and lets the owner copy it again later.
 *
 * @param permission always VIEW in Phase 4; set by the server, never taken from the client
 * @param viewCount  successful openings of the public page through this link
 */
public record ShareLinkResponse(
        Long id,
        String token,
        SharePermission permission,
        int viewCount,
        Instant createdAt) {
}
