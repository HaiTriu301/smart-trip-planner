package com.trieu.tripplanner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/v1/trips/{tripId}/comments (design.md 10.2 "Quy ước Comment API"). Only shape rules live here
 * (CLAUDE.md rule 13).
 * <p>
 * Not in the body on purpose: the author is the signed-in user (CLAUDE.md rule 16) and the trip comes from the URL.
 *
 * @param content plain text, line breaks kept; stored trimmed. The length is measured before trimming, like the
 *                title of an activity
 */
public record CreateCommentRequest(
        @NotBlank(message = "{validation.comment.content.required}")
        @Size(max = 2000, message = "{validation.comment.content.too-long}")
        String content) {
}
