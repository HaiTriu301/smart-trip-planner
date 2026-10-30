package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

/**
 * A save made on a version that is no longer the stored one: somebody saved the same trip or activity after the
 * client loaded it → 409 STALE_VERSION, nothing written (design.md 11.5, rule 14.29). The client keeps what the
 * user typed, reloads and may save again on purpose with the new version.
 */
public class StaleVersionException extends AppException {

    public StaleVersionException(String entityName, Long id, Long sentVersion, Long storedVersion) {
        super(ErrorCode.STALE_VERSION, "%s %d: client sent version %d, stored version is %d"
                .formatted(entityName, id, sentVersion, storedVersion));
    }

}
