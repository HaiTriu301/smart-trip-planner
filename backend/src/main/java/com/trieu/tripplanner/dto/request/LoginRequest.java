package com.trieu.tripplanner.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /api/v1/auth/login. No strength rules on password here: any string is compared against the hash.
 */
public record LoginRequest(

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        String email,

        @NotBlank(message = "{validation.password.required}")
        String password) {
}
