package com.trieu.tripplanner.dto.request;

import com.trieu.tripplanner.common.validation.PasswordConfirmation;
import com.trieu.tripplanner.common.validation.PasswordConfirmed;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/v1/auth/register. Only shape/format rules live here (CLAUDE.md rule 13);
 * "email already taken" is a business rule checked in AuthService.
 * Messages are keys resolved from messages.properties (Vietnamese, rule 14).
 * {@code confirmPassword} is validated and then dropped: it is never stored or hashed.
 */
@PasswordConfirmed
public record RegisterRequest(

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        @Size(max = 255, message = "{validation.email.too-long}")
        String email,

        // Printable ASCII only: keeps the password under BCrypt's 72-byte input limit
        // and rules out whitespace that users cannot see when typing.
        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, max = 72, message = "{validation.password.length}")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[\\x21-\\x7E]+$",
                 message = "{validation.password.weak}")
        String password,

        @NotBlank(message = "{validation.password.confirm-required}")
        String confirmPassword,

        @NotBlank(message = "{validation.full-name.required}")
        @Size(max = 120, message = "{validation.full-name.too-long}")
        String fullName) implements PasswordConfirmation {
}
