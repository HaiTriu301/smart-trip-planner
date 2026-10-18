package com.trieu.tripplanner.support;

import com.trieu.tripplanner.dto.request.CreateCommentRequest;

/**
 * The one place where tests call the constructor of the comment request. The record has no builder, so a new field
 * changes the constructor: with every test going through here, only this class has to follow, and tests that do
 * not care about the new field stay as they are.
 */
public final class TestComments {

    private TestComments() {
    }

    /** Body of POST .../comments: a root comment on the whole trip. */
    public static CreateCommentRequest createRequest(String content) {
        return new CreateCommentRequest(content, null, null);
    }

    /** The same request, about one activity of the trip. */
    public static CreateCommentRequest onActivity(CreateCommentRequest request, Long activityId) {
        return new CreateCommentRequest(request.content(), activityId, request.parentId());
    }

    /** The same request, as a reply to a comment. */
    public static CreateCommentRequest replyTo(CreateCommentRequest request, Long parentId) {
        return new CreateCommentRequest(request.content(), request.activityId(), parentId);
    }

}
