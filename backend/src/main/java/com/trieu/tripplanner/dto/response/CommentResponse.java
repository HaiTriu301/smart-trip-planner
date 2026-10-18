package com.trieu.tripplanner.dto.response;

import java.time.Instant;

/**
 * One comment of a trip (design.md 10.2 "Quy ước Comment API"). The list is flat: {@code parentId} lets the screen
 * nest a reply under its root, one level deep.
 *
 * @param activityId the activity the comment is about; null for a comment on the whole trip
 * @param parentId   the root comment this one replies to; null for a root comment
 * @param author     who wrote it; nothing of the account but what a fellow traveller may see
 * @param content    trimmed plain text
 */
public record CommentResponse(
        Long id,
        Long activityId,
        Long parentId,
        Author author,
        String content,
        Instant createdAt) {

    /** The author of a comment: no email, no password hash can get here because there is no field for them. */
    public record Author(Long id, String fullName, String avatarUrl) {
    }

}
