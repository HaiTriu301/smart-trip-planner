package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.response.MemberResponse;
import com.trieu.tripplanner.model.TripMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * TripMember → DTO, generated at compile time. unmappedTargetPolicy=ERROR: a new response field without a source
 * fails the build. {@code role} is mapped by constant name (MemberRole.EDITOR → TripRole.EDITOR); OWNER never
 * comes from a row.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MemberMapper {

    // user may be null (invited email without an account): MapStruct then leaves the three account fields null.
    // When it is set, the callers fetched it together with the row, so no extra query happens here
    @Mapping(target = "memberId", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")
    @Mapping(target = "email", source = "invitedEmail")
    MemberResponse toResponse(TripMember member);

}
