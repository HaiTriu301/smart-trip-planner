package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

/**
 * The credentials were right but an admin set the account to BLOCKED → 403 ACCOUNT_BLOCKED.
 */
public class AccountBlockedException extends AppException {

    public AccountBlockedException(Long userId) {
        super(ErrorCode.ACCOUNT_BLOCKED, "Account %d is blocked".formatted(userId));
    }

}
