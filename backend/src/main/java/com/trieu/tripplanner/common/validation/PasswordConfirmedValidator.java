package com.trieu.tripplanner.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordConfirmedValidator implements ConstraintValidator<PasswordConfirmed, PasswordConfirmation> {

    @Override
    public boolean isValid(PasswordConfirmation value, ConstraintValidatorContext context) {
        if (value == null || value.password() == null || value.confirmPassword() == null) {
            // Missing values are reported by @NotBlank on the fields themselves; do not double-report here
            return true;
        }
        if (value.password().equals(value.confirmPassword())) {
            return true;
        }
        // Attach the violation to confirmPassword instead of the whole object
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("confirmPassword")
                .addConstraintViolation();
        return false;
    }

}
