package com.trieu.tripplanner.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /api/v1/trips/{tripId}/members/accept: the token the /invite page copied from the mail link.
 * The trip comes from the path, so the page can show which trip is being joined before the call.
 */
public record AcceptInvitationRequest(
        @NotBlank(message = "{validation.token.required}")
        String token) {
}
