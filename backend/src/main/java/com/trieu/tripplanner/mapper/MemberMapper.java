package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.response.MemberResponse;
import com.trieu.tripplanner.model.TripMember;
import com.trieu.tripplanner.model.User;
import java.util.List;
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

    List<MemberResponse> toResponses(List<TripMember> members);

    /**
     * The owner as the first line of the member list (design.md 6.2: the owner has no row, so there is no
     * memberId and no invitation dates; the role and status are what the owner is by definition).
     */
    @Mapping(target = "memberId", ignore = true)
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "role", constant = "OWNER")
    @Mapping(target = "status", constant = "ACCEPTED")
    @Mapping(target = "invitedAt", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    MemberResponse toOwnerResponse(User owner);

}
