package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

/**
 * Every random slug candidate was already taken. With 36^6 suffixes per title this should never happen;
 * if it does, the client gets a plain 500 and the log says why.
 */
public class SlugGenerationException extends AppException {

    public SlugGenerationException(int attempts) {
        super(ErrorCode.INTERNAL_ERROR, "No free trip slug after %d attempts".formatted(attempts));
    }

}
