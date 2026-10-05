package com.trieu.tripplanner.exception;

import com.trieu.tripplanner.common.constant.ErrorCode;

/**
 * A third-party service did not give a usable answer: it was unreachable, too slow, answered with an error
 * status, or sent something that cannot be read (design.md 7.3). Real providers turn every such failure into
 * this exception, so services never see an exception of the HTTP library.
 */
public class ProviderUnavailableException extends AppException {

    /**
     * @param source which service failed, for the log (for example {@code open-meteo})
     * @param detail what went wrong, in English, for the log
     */
    public ProviderUnavailableException(String source, String detail) {
        super(ErrorCode.PROVIDER_UNAVAILABLE, "Provider " + source + " is unavailable: " + detail);
    }

    public ProviderUnavailableException(String source, String detail, Throwable cause) {
        this(source, detail);
        initCause(cause);
    }

}
