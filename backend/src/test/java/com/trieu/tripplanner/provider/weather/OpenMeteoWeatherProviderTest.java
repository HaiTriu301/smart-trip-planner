package com.trieu.tripplanner.provider.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.config.properties.ProviderProperties;
import com.trieu.tripplanner.exception.ProviderUnavailableException;
import com.trieu.tripplanner.provider.weather.dto.DailyForecast;
import com.trieu.tripplanner.provider.weather.dto.WeatherCondition;
import com.trieu.tripplanner.support.StubHttpServer;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * No Spring context and no network: the provider is built by hand on a RestClient whose calls are answered by
 * MockRestServiceServer. The sample file is a real answer of the service, saved on 2026-10-05 for Da Nang.
 */
class OpenMeteoWeatherProviderTest {

    private static final String BASE_URL = "https://weather.test";
    private static final String USER_AGENT = "trip-planner-test/1.0";

    private static final BigDecimal DA_NANG_LAT = new BigDecimal("16.0678000");
    private static final BigDecimal DA_NANG_LNG = new BigDecimal("108.2208000");

    /** The sample holds 2026-10-05 to 2026-10-20. */
    private static final LocalDate FIRST_DAY_OF_SAMPLE = LocalDate.of(2026, 10, 5);
    private static final LocalDate LAST_DAY_OF_SAMPLE = LocalDate.of(2026, 10, 20);
    private static final ClassPathResource SAMPLE = new ClassPathResource("provider/open-meteo/forecast-16-days.json");

    private MockRestServiceServer server;
    private OpenMeteoWeatherProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new OpenMeteoWeatherProvider(builder, properties(BASE_URL));
    }

    @Test
    void readsTheDaysAskedForOutOfARealAnswer() {
        server.expect(requestTo(startsWith(BASE_URL + "/v1/forecast")))
                .andRespond(withSuccess(SAMPLE, MediaType.APPLICATION_JSON));

        List<DailyForecast> forecasts = provider.forecast(DA_NANG_LAT, DA_NANG_LNG,
                LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 8));

        assertThat(forecasts).containsExactly(
                new DailyForecast(LocalDate.of(2026, 10, 6), WeatherCondition.RAIN, 23.8, 29.0, 98),
                new DailyForecast(LocalDate.of(2026, 10, 7), WeatherCondition.THUNDERSTORM, 24.0, 26.6, 100),
                new DailyForecast(LocalDate.of(2026, 10, 8), WeatherCondition.THUNDERSTORM, 24.6, 26.3, 100));
    }

    @Test
    void asksForTheWholeForecastOfThePointAndSaysWhoIsCalling() {
        server.expect(requestTo(startsWith(BASE_URL + "/v1/forecast")))
                .andExpect(method(HttpMethod.GET))
                // Seven decimals come in, four go out
                .andExpect(queryParam("latitude", "16.0678"))
                .andExpect(queryParam("longitude", "108.2208"))
                .andExpect(queryParam("daily",
                        "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"))
                .andExpect(queryParam("timezone", "auto"))
                .andExpect(queryParam("forecast_days", "16"))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(withSuccess(SAMPLE, MediaType.APPLICATION_JSON));

        provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY_OF_SAMPLE, LAST_DAY_OF_SAMPLE);

        server.verify();
    }

    @Test
    void daysTheServiceDoesNotHaveAreMissingNotAnError() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withSuccess(SAMPLE, MediaType.APPLICATION_JSON));

        // Two days inside the sample, two days after its end
        List<DailyForecast> forecasts = provider.forecast(DA_NANG_LAT, DA_NANG_LNG,
                LAST_DAY_OF_SAMPLE.minusDays(1), LAST_DAY_OF_SAMPLE.plusDays(2));

        assertThat(forecasts).extracting(DailyForecast::date)
                .containsExactly(LAST_DAY_OF_SAMPLE.minusDays(1), LAST_DAY_OF_SAMPLE);
    }

    @Test
    void rangeOutsideTheForecastGivesAnEmptyList() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withSuccess(SAMPLE, MediaType.APPLICATION_JSON));

        assertThat(provider.forecast(DA_NANG_LAT, DA_NANG_LNG,
                LAST_DAY_OF_SAMPLE.plusDays(1), LAST_DAY_OF_SAMPLE.plusDays(3))).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "0, CLEAR", "1, CLEAR", "2, PARTLY_CLOUDY", "3, CLOUDY",
            "45, FOG", "48, FOG",
            "51, RAIN", "53, RAIN", "55, RAIN", "56, RAIN", "57, RAIN",
            "61, RAIN", "63, RAIN", "65, RAIN", "66, RAIN", "67, RAIN",
            "80, RAIN", "81, RAIN", "82, RAIN",
            "71, SNOW", "73, SNOW", "75, SNOW", "77, SNOW", "85, SNOW", "86, SNOW",
            "95, THUNDERSTORM", "96, THUNDERSTORM", "99, THUNDERSTORM"
    })
    void everyWeatherCodeOfTheServiceBecomesOneOfTheSevenConditions(int wmoCode, WeatherCondition expected) {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withSuccess(oneDay(String.valueOf(wmoCode), "24.0", "30.0", "40"),
                        MediaType.APPLICATION_JSON));

        assertThat(provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY_OF_SAMPLE, FIRST_DAY_OF_SAMPLE))
                .containsExactly(new DailyForecast(FIRST_DAY_OF_SAMPLE, expected, 24.0, 30.0, 40));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // weather code | lowest | highest | chance of rain
            "null | 24.0 | 30.0 | 40",
            "3    | null | 30.0 | 40",
            "3    | 24.0 | null | 40",
            "3    | 24.0 | 30.0 | null",
            // 4 is not in the table of the service
            "4    | 24.0 | 30.0 | 40"
    })
    void dayWithAnEmptyValueOrAnUnknownCodeIsLeftOut(String code, String tempMin, String tempMax, String rain) {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withSuccess(oneDay(code, tempMin, tempMax, rain), MediaType.APPLICATION_JSON));

        assertThat(provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY_OF_SAMPLE, FIRST_DAY_OF_SAMPLE)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 429, 500, 502, 503})
    void errorStatusOfTheServiceBecomesProviderUnavailable(int status) {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withStatus(HttpStatus.valueOf(status))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":true,\"reason\":\"sample\"}"));

        assertUnavailable();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not json at all",
            "{}",
            "{\"daily\":null}",
            // No list of days
            "{\"daily\":{\"weather_code\":[3]}}",
            // Two days but one value: positions cannot be trusted
            "{\"daily\":{\"time\":[\"2026-10-05\",\"2026-10-06\"],\"weather_code\":[3],"
                    + "\"temperature_2m_max\":[30.0],\"temperature_2m_min\":[24.0],"
                    + "\"precipitation_probability_max\":[40]}}"
    })
    void answerThatCannotBeReadBecomesProviderUnavailable(String body) {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertUnavailable();
    }

    @Test
    void serviceThatDoesNotAnswerInTimeBecomesProviderUnavailable() {
        try (StubHttpServer silent = StubHttpServer.neverAnswering()) {
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
            requestFactory.setReadTimeout(Duration.ofMillis(200));
            provider = new OpenMeteoWeatherProvider(RestClient.builder().requestFactory(requestFactory),
                    properties(silent.baseUrl()));

            assertUnavailable();
        }
    }

    @Test
    void serviceThatCannotBeReachedBecomesProviderUnavailable() {
        String addressNobodyListensOn;
        try (StubHttpServer stopped = StubHttpServer.neverAnswering()) {
            addressNobodyListensOn = stopped.baseUrl();
        }
        provider = new OpenMeteoWeatherProvider(RestClient.builder(), properties(addressNobodyListensOn));

        assertUnavailable();
    }

    private void assertUnavailable() {
        assertThatThrownBy(() ->
                provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY_OF_SAMPLE, LAST_DAY_OF_SAMPLE))
                .isInstanceOfSatisfying(ProviderUnavailableException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_UNAVAILABLE);
                    assertThat(ex.getMessage()).contains("open-meteo");
                });
    }

    private static ProviderProperties properties(String baseUrl) {
        return new ProviderProperties(USER_AGENT, new ProviderProperties.OpenMeteo(baseUrl));
    }

    /** An answer in the shape of the service with a single day, 2026-10-05; "null" leaves a value empty. */
    private static String oneDay(String code, String tempMin, String tempMax, String rain) {
        return """
                {"latitude":16.063269,"longitude":108.23864,"timezone":"Asia/Ho_Chi_Minh",
                 "daily":{"time":["2026-10-05"],"weather_code":[%s],"temperature_2m_max":[%s],
                          "temperature_2m_min":[%s],"precipitation_probability_max":[%s]}}
                """.formatted(code.trim(), tempMax.trim(), tempMin.trim(), rain.trim());
    }

}
