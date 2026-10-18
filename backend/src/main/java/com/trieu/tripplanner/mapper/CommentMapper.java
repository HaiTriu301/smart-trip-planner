package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.response.CommentResponse;
import com.trieu.tripplanner.model.Comment;
import com.trieu.tripplanner.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Comment → DTO, generated at compile time. unmappedTargetPolicy=ERROR: a new response field without a source
 * fails the build.
 * <p>
 * {@code activityId} and {@code parentId} read only the id of the association, which a Hibernate proxy knows
 * without a query. The author is read in full: the callers make sure it is loaded.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CommentMapper {

    @Mapping(target = "activityId", source = "activity.id")
    @Mapping(target = "parentId", source = "parent.id")
    CommentResponse toResponse(Comment comment);

    CommentResponse.Author toAuthor(User author);

}
