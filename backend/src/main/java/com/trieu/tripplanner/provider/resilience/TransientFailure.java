package com.trieu.tripplanner.provider.resilience;

import com.trieu.tripplanner.exception.ProviderUnavailableException;
import java.util.function.Predicate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * The failures of an outside service worth a second try (design.md 7.3): it answered with a 5xx status, did not
 * answer in time, or could not be reached. These pass by themselves. Everything else is not retried: a 4xx
 * status says the request itself is wrong, and an answer that cannot be read will be just as unreadable the
 * next time.
 * <p>
 * Named in application.yml as the retry predicate; Resilience4j creates it with the constructor without
 * arguments.
 */
public class TransientFailure implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable failure) {
        if (!(failure instanceof ProviderUnavailableException)) {
            return false;
        }
        Throwable cause = failure.getCause();
        return cause instanceof HttpServerErrorException || cause instanceof ResourceAccessException;
    }

}
