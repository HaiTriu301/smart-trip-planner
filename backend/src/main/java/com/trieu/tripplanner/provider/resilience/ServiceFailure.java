package com.trieu.tripplanner.provider.resilience;

import com.trieu.tripplanner.exception.ProviderUnavailableException;
import java.util.function.Predicate;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * The failures that say something about the health of an outside service, and therefore count towards opening
 * its circuit (design.md 7.3): the HTTP call itself failed. A 4xx status does not count, the service is fine and
 * the question was wrong (OSRM answers 400 when there is no road between two points); except 429, the service
 * saying it gets too many calls. A failure without an HTTP cause does not count either: the circuit being open,
 * our own rate limit, an answer that was read but has not the expected content.
 * <p>
 * Named in application.yml as the predicate of the circuit breakers.
 */
public class ServiceFailure implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable failure) {
        if (!(failure instanceof ProviderUnavailableException)
                || !(failure.getCause() instanceof RestClientException cause)) {
            return false;
        }
        if (cause instanceof HttpClientErrorException refused) {
            return refused.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value();
        }
        return true;
    }

}
