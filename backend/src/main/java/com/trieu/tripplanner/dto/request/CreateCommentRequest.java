package com.trieu.tripplanner.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/v1/trips/{tripId}/comments (design.md 10.2 "Quy ước Comment API"). Only shape rules live here
 * (CLAUDE.md rule 13); whether the activity and the parent belong to the trip is checked by the service.
 * <p>
 * Not in the body on purpose: the author is the signed-in user (CLAUDE.md rule 16) and the trip comes from the URL.
 *
 * @param content    plain text, line breaks kept; stored trimmed. The length is measured before trimming, like the
 *                   title of an activity
 * @param activityId the activity the comment is about; null for a comment on the whole trip. An activity of
 *                   another trip, or one that does not exist, is a 404. Ignored on a reply
 * @param parentId   the root comment this one answers; null for a root comment. Replies go one level deep: the
 *                   parent must be a live root comment of the same trip, otherwise 400 on this field. A reply sits
 *                   on the activity of its parent
 */
public record CreateCommentRequest(
        @NotBlank(message = "{validation.comment.content.required}")
        @Size(max = 2000, message = "{validation.comment.content.too-long}")
        String content,

        Long activityId,

        Long parentId) {
}
