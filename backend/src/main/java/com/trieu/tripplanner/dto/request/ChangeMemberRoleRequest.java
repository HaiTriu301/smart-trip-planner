package com.trieu.tripplanner.dto.request;

import com.trieu.tripplanner.model.enums.MemberRole;
import jakarta.validation.constraints.NotNull;

/**
 * Body of PATCH /api/v1/trips/{tripId}/members/{memberId} (design.md 10.2 "Sharing"). The only thing the owner
 * may change on a member is the role; as with inviting, OWNER is not a {@link MemberRole}, so ownership can
 * never be handed over here (design.md rule 14.7).
 */
public record ChangeMemberRoleRequest(
        @NotNull(message = "{validation.member.role.required}")
        MemberRole role) {
}
