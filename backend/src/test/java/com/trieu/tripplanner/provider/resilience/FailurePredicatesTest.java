package com.trieu.tripplanner.provider.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.exception.ProviderUnavailableException;
import java.net.SocketTimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * Which failures of an outside service are tried again, and which count towards opening its circuit
 * (design.md 7.3). The failures are built the way the providers build them: a ProviderUnavailableException
 * around what the HTTP client threw.
 */
class FailurePredicatesTest {

    private final TransientFailure retried = new TransientFailure();
    private final ServiceFailure counted = new ServiceFailure();

    static Stream<Arguments> failures() {
        return Stream.of(
                //            what happened                                                  retried  counted
                Arguments.of("500", around(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)), true, true),
                Arguments.of("503", around(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE)), true, true),
                Arguments.of("no answer in time", around(new ResourceAccessException("Read timed out",
                        new SocketTimeoutException())), true, true),
                Arguments.of("400", around(new HttpClientErrorException(HttpStatus.BAD_REQUEST)), false, false),
                Arguments.of("403", around(new HttpClientErrorException(HttpStatus.FORBIDDEN)), false, false),
                Arguments.of("404", around(new HttpClientErrorException(HttpStatus.NOT_FOUND)), false, false),
                // The service says it gets too many calls: asking again at once makes it worse, but it does count
                Arguments.of("429", around(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS)), false, true),
                Arguments.of("unreadable answer", around(new RestClientException("Error while extracting response")),
                        false, true),
                // Read, but without the expected content; the circuit being open; our own rate limit
                Arguments.of("no HTTP cause", new ProviderUnavailableException("osrm", "the answer has no route"),
                        false, false));
    }

    @ParameterizedTest(name = "{0}: tried again {2}, counted against the service {3}")
    @MethodSource("failures")
    void failureIsTriedAgainAndCountedOnlyWhenItSaysSomethingAboutTheService(String what, Throwable failure,
            boolean triedAgain, boolean countedAgainstTheService) {
        assertThat(retried.test(failure)).isEqualTo(triedAgain);
        assertThat(counted.test(failure)).isEqualTo(countedAgainstTheService);
    }

    @Test
    void exceptionThatIsNotAProviderFailureIsNeitherTriedAgainNorCounted() {
        // A bug of ours must not be hidden behind three attempts, nor open the circuit of a healthy service
        RuntimeException bug = new IllegalStateException("bug", new HttpServerErrorException(HttpStatus.BAD_GATEWAY));

        assertThat(retried.test(bug)).isFalse();
        assertThat(counted.test(bug)).isFalse();
    }

    private static ProviderUnavailableException around(RestClientException cause) {
        return new ProviderUnavailableException("photon", cause.getMessage(), cause);
    }

}
