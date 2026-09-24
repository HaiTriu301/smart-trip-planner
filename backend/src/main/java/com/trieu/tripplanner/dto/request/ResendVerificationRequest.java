package com.trieu.tripplanner.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /api/v1/auth/resend-verification. Public endpoint: the response is the same whether or not
 * the email exists (design.md 14.15).
 */
public record ResendVerificationRequest(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        String email) {
}
