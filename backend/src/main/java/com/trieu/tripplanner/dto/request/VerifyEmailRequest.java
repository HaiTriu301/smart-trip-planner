package com.trieu.tripplanner.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /api/v1/auth/verify-email: the token copied from the link by the frontend page.
 */
public record VerifyEmailRequest(
        @NotBlank(message = "{validation.token.required}")
        String token) {
}
