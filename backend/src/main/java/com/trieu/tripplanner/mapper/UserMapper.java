package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Entity → DTO mapping, generated at compile time (build/generated/sources/annotationProcessor).
 * componentModel=spring comes from the compiler flag in build.gradle, so the implementation is a bean.
 * unmappedTargetPolicy=ERROR: adding a field to UserResponse without a source fails the build instead of returning null.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

    UserResponse toResponse(User user);
}
