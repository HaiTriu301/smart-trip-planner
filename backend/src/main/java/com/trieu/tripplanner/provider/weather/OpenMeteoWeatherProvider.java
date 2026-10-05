package com.trieu.tripplanner.provider.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trieu.tripplanner.config.properties.ProviderProperties;
import com.trieu.tripplanner.exception.ProviderUnavailableException;
import com.trieu.tripplanner.provider.weather.dto.DailyForecast;
import com.trieu.tripplanner.provider.weather.dto.WeatherCondition;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Real weather source: the forecast API of Open-Meteo (design.md 7.2). Free for non-commercial use, no API key.
 * Active when {@code app.providers.weather=open-meteo}.
 * <p>
 * The service is always asked for everything it has, today and the fifteen days after it, and the days outside
 * the range wanted are dropped here. Asking for an exact range is refused as a whole (HTTP 400) as soon as one
 * day lies outside what the service has, and "today" at the destination can differ by one day from "today" of
 * the user who asks. A day the service does not have is simply missing from the result, which the port allows.
 * <p>
 * Days are calendar days at the point asked for ({@code timezone=auto}).
 * <p>
 * A failed call is tried again and repeated failures open a circuit (design.md 7.3; the numbers are in
 * application.yml under {@code resilience4j}). Both work through the proxy Spring puts in front of this bean.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.providers.weather", havingValue = "open-meteo")
public class OpenMeteoWeatherProvider implements WeatherProvider {

    /** The most the forecast API gives: today and the fifteen days after it. */
    static final int FORECAST_DAYS = 16;

    /** More decimals than this say nothing about the weather (about 11 m), same as the cache key. */
    static final int COORDINATE_SCALE = 4;

    private static final String SOURCE = "open-meteo";
    private static final String DAILY_VALUES =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max";

    private final RestClient restClient;

    /**
     * @param builder the application's builder: it carries the time limits of {@code spring.http.clients.*}
     */
    public OpenMeteoWeatherProvider(RestClient.Builder builder, ProviderProperties properties) {
        this.restClient = builder
                .baseUrl(properties.openMeteo().baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .build();
    }

    /**
     * @throws ProviderUnavailableException the service could not be reached, took too long, answered with an
     *                                      error status or sent something unreadable
     */
    @Override
    @Retry(name = SOURCE)
    @CircuitBreaker(name = SOURCE, fallbackMethod = "suspended")
    public List<DailyForecast> forecast(BigDecimal lat, BigDecimal lng, LocalDate from, LocalDate to) {
        Daily daily = ask(lat, lng);
        List<DailyForecast> forecasts = new ArrayList<>();
        for (int i = 0; i < daily.time().size(); i++) {
            LocalDate date = daily.time().get(i);
            if (date == null || date.isBefore(from) || date.isAfter(to)) {
                continue;
            }
            toForecast(daily, i).ifPresent(forecasts::add);
        }
        return List.copyOf(forecasts);
    }

    /**
     * What Resilience4j calls instead of {@link #forecast} while the circuit is open. No cause on purpose: a
     * call that was never made is neither retried nor counted against the service. Public on purpose: the
     * library cannot safely call a private fallback from two threads at once (BUG-PLAT-004).
     */
    public List<DailyForecast> suspended(CallNotPermittedException ex) {
        throw new ProviderUnavailableException(SOURCE, "calls are suspended after repeated failures");
    }

    /** One call for the whole forecast of the point; every failure becomes a ProviderUnavailableException. */
    private Daily ask(BigDecimal lat, BigDecimal lng) {
        ForecastAnswer answer;
        try {
            answer = restClient.get()
                    .uri(uri -> uri.path("/v1/forecast")
                            .queryParam("latitude", rounded(lat))
                            .queryParam("longitude", rounded(lng))
                            .queryParam("daily", DAILY_VALUES)
                            .queryParam("timezone", "auto")
                            .queryParam("forecast_days", FORECAST_DAYS)
                            .build())
                    .retrieve()
                    .body(ForecastAnswer.class);
        }
        catch (RestClientException ex) {
            // Error status, no connection, time limit reached, body that is not the expected JSON
            throw new ProviderUnavailableException(SOURCE, ex.getMessage(), ex);
        }
        if (answer == null || answer.daily() == null || !answer.daily().isConsistent()) {
            throw new ProviderUnavailableException(SOURCE, "the answer has no usable daily forecast");
        }
        return answer.daily();
    }

    /**
     * The forecast of the day at one position of the answer, or nothing when the service left a value of that
     * day empty or reports a weather code this application does not know: a day without forecast is better
     * than a made-up one.
     */
    private static Optional<DailyForecast> toForecast(Daily daily, int index) {
        Integer code = daily.weatherCode().get(index);
        Double tempMin = daily.tempMin().get(index);
        Double tempMax = daily.tempMax().get(index);
        Integer precipitationProbability = daily.precipitationProbability().get(index);
        if (code == null || tempMin == null || tempMax == null || precipitationProbability == null) {
            return Optional.empty();
        }
        Optional<WeatherCondition> condition = conditionOf(code);
        if (condition.isEmpty()) {
            log.warn("Open-Meteo reported an unknown weather code {} for {}", code, daily.time().get(index));
        }
        return condition.map(value ->
                new DailyForecast(daily.time().get(index), value, tempMin, tempMax, precipitationProbability));
    }

    /**
     * WMO weather code (the table in the Open-Meteo documentation) to one of the seven conditions of the API
     * (design.md 10.2). Drizzle, rain and rain showers are all RAIN; snow and snow showers are SNOW.
     */
    static Optional<WeatherCondition> conditionOf(int wmoCode) {
        return Optional.ofNullable(switch (wmoCode) {
            case 0, 1 -> WeatherCondition.CLEAR;                       // clear sky, mainly clear
            case 2 -> WeatherCondition.PARTLY_CLOUDY;
            case 3 -> WeatherCondition.CLOUDY;                         // overcast
            case 45, 48 -> WeatherCondition.FOG;
            case 51, 53, 55, 56, 57,                                   // drizzle, freezing drizzle
                 61, 63, 65, 66, 67,                                   // rain, freezing rain
                 80, 81, 82 -> WeatherCondition.RAIN;                  // rain showers
            case 71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW;      // snow fall, snow grains, snow showers
            case 95, 96, 99 -> WeatherCondition.THUNDERSTORM;
            default -> null;
        });
    }

    private static String rounded(BigDecimal degrees) {
        return degrees.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    /** The part of the answer this application reads. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ForecastAnswer(Daily daily) {
    }

    /**
     * One list per value, all in the same order as {@code time}: position {@code i} of every list belongs to
     * day {@code time[i]}. The service puts null where it has no value.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Daily(
            List<LocalDate> time,
            @JsonProperty("weather_code") List<Integer> weatherCode,
            @JsonProperty("temperature_2m_min") List<Double> tempMin,
            @JsonProperty("temperature_2m_max") List<Double> tempMax,
            @JsonProperty("precipitation_probability_max") List<Integer> precipitationProbability) {

        /** Every list is there and as long as the list of days; otherwise positions cannot be trusted. */
        boolean isConsistent() {
            return time != null && sameLength(weatherCode) && sameLength(tempMin) && sameLength(tempMax)
                    && sameLength(precipitationProbability);
        }

        private boolean sameLength(List<?> values) {
            return values != null && values.size() == time.size();
        }
    }

}
