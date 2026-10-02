package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.response.ForecastResponse;
import com.trieu.tripplanner.dto.response.TripWeatherDayResponse;
import com.trieu.tripplanner.dto.response.TripWeatherResponse;
import com.trieu.tripplanner.dto.response.TripWeatherStatus;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.provider.weather.dto.WeatherCondition;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.security.permission.TripPermissionEvaluator;
import com.trieu.tripplanner.service.WeatherService;
import com.trieu.tripplanner.support.TestUsers;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Web layer only: WeatherService and the tripPermission bean are mocks.
 */
@WebMvcTest(WeatherController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class WeatherControllerTest {

    private static final long TRIP_ID = 5L;
    private static final String WEATHER_URL = "/api/v1/weather/trips/5";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private WeatherService weatherService;

    // Same bean name the SpEL expressions reference: @tripPermission
    @MockitoBean(name = "tripPermission")
    private TripPermissionEvaluator tripPermission;

    private String bearer;

    @BeforeEach
    void signIn() {
        bearer = "Bearer " + jwtTokenProvider.generateAccessToken(TestUsers.verified(7L, "an@example.com")).token();
    }

    @Test
    void returnsEachDayWithItsForecastWhenViewAllowed() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(weatherService.forTrip(TRIP_ID)).thenReturn(new TripWeatherResponse(TripWeatherStatus.OK, List.of(
                new TripWeatherDayResponse(11L, LocalDate.of(2026, 10, 5),
                        new ForecastResponse(WeatherCondition.RAIN, 24.1, 29.6, 70)),
                new TripWeatherDayResponse(12L, LocalDate.of(2026, 10, 6), null))));

        assertThat(mvc.get().uri(WEATHER_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "status": "OK", "days": [
                              { "dayId": 11, "date": "2026-10-05",
                                "forecast": { "condition": "RAIN", "tempMin": 24.1, "tempMax": 29.6,
                                              "precipitationProbability": 70 } },
                              { "dayId": 12, "date": "2026-10-06", "forecast": null } ] } }
                        """);
    }

    @Test
    void tripWithoutDestinationIsAnAnswerNotAnError() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(weatherService.forTrip(TRIP_ID)).thenReturn(new TripWeatherResponse(TripWeatherStatus.NO_DESTINATION,
                List.of(new TripWeatherDayResponse(11L, LocalDate.of(2026, 10, 5), null))));

        assertThat(mvc.get().uri(WEATHER_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "status": "NO_DESTINATION",
                                    "days": [ { "dayId": 11, "date": "2026-10-05", "forecast": null } ] } }
                        """);
    }

    @Test
    void withoutTokenReturns401() {
        assertThat(mvc.get().uri(WEATHER_URL))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(weatherService);
    }

    @Test
    void returns403WhenViewDeniedAndNeverReachesService() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(mvc.get().uri(WEATHER_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        verify(weatherService, never()).forTrip(any());
    }

    @Test
    void returns404WhenTripDoesNotExist() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(weatherService.forTrip(TRIP_ID)).thenThrow(new ResourceNotFoundException("Trip", TRIP_ID));

        assertThat(mvc.get().uri(WEATHER_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void nonNumericTripIdReturns400() {
        assertThat(mvc.get().uri("/api/v1/weather/trips/abc").header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(weatherService);
    }

}
