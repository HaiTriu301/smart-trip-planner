package com.trieu.tripplanner.dto.request;

import com.trieu.tripplanner.common.validation.PasswordConfirmation;
import com.trieu.tripplanner.common.validation.PasswordConfirmed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/v1/auth/reset-password: the token from the mailed link plus the new password typed twice.
 * Same strength rules as registration (design.md 14.13); {@code confirmPassword} is checked and dropped.
 */
@PasswordConfirmed
public record ResetPasswordRequest(

        @NotBlank(message = "{validation.token.required}")
        String token,

        @NotBlank(message = "{validation.password.required}")
        @Size(min = 8, max = 72, message = "{validation.password.length}")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[\\x21-\\x7E]+$",
                 message = "{validation.password.weak}")
        String newPassword,

        @NotBlank(message = "{validation.password.confirm-required}")
        String confirmPassword) implements PasswordConfirmation {

    /** PasswordConfirmation reads {@code password()}; this DTO names the field newPassword for clarity. */
    @Override
    public String password() {
        return newPassword;
    }

}
