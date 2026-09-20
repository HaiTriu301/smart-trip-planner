package com.trieu.tripplanner.common.validation;

/**
 * Implemented by request DTOs that carry a password and its confirmation, so one validator serves them all.
 */
public interface PasswordConfirmation {

    String password();

    String confirmPassword();

}
