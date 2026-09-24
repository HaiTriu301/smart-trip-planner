package com.trieu.tripplanner.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /api/v1/auth/forgot-password. Public; the response never reveals whether the email exists
 * (design.md 14.15). A reset mail is sent only to verified, active accounts (14.12).
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        String email) {
}
