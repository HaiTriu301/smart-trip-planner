package com.trieu.tripplanner.dto.request;

import com.trieu.tripplanner.model.enums.MemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/v1/trips/{tripId}/members (design.md 10.2 "Quy ước Sharing API"). Only shape rules live
 * here (CLAUDE.md rule 13); "this is the owner's own email" and "already a member" are checked by SharingService.
 * The role is a {@link MemberRole}, so OWNER is not even parseable: nobody can invite a second owner.
 */
public record InviteMemberRequest(

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        @Size(max = 255, message = "{validation.email.too-long}")
        String email,

        @NotNull(message = "{validation.member.role.required}")
        MemberRole role) {
}
