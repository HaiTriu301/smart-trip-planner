package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.request.UpdateTripDayRequest;
import com.trieu.tripplanner.dto.response.TripDayResponse;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.security.permission.TripPermissionEvaluator;
import com.trieu.tripplanner.service.TripDayService;
import com.trieu.tripplanner.support.TestUsers;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Web layer only: TripDayService and the tripPermission bean are mocks.
 */
@WebMvcTest(TripDayController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class TripDayControllerTest {

    private static final long TRIP_ID = 5L;
    private static final String DAYS_URL = "/api/v1/trips/5/days";
    private static final long DAY_ID = 11L;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TripDayService tripDayService;

    // Same bean name the SpEL expressions reference: @tripPermission
    @MockitoBean(name = "tripPermission")
    private TripPermissionEvaluator tripPermission;

    private String bearer;

    @BeforeEach
    void signIn() {
        bearer = "Bearer " + jwtTokenProvider.generateAccessToken(TestUsers.verified(7L, "an@example.com")).token();
    }

    @Test
    void listReturnsDaysInOrderWhenViewAllowed() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(tripDayService.list(TRIP_ID)).thenReturn(List.of(
                new TripDayResponse(11L, 1, LocalDate.of(2026, 10, 1), "Đến nơi", "Nhận phòng"),
                new TripDayResponse(12L, 2, LocalDate.of(2026, 10, 2), null, null)));

        assertThat(mvc.get().uri(DAYS_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": [ { "id": 11, "dayIndex": 1, "date": "2026-10-01", "title": "Đến nơi", "note": "Nhận phòng" },
                                    { "id": 12, "dayIndex": 2, "date": "2026-10-02", "title": null } ] }
                        """);
    }

    @Test
    void listWithoutTokenReturns401() {
        assertThat(mvc.get().uri(DAYS_URL))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(tripDayService);
    }

    @Test
    void listReturns403WhenViewDeniedAndNeverReachesService() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(mvc.get().uri(DAYS_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        verify(tripDayService, never()).list(any());
    }

    @Test
    void listReturns404WhenTripDoesNotExist() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(tripDayService.list(TRIP_ID)).thenThrow(new ResourceNotFoundException("Trip", TRIP_ID));

        assertThat(mvc.get().uri(DAYS_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void updateReturnsTheDayWhenEditAllowed() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(tripDayService.update(eq(TRIP_ID), eq(DAY_ID), any()))
                .thenReturn(new TripDayResponse(DAY_ID, 1, LocalDate.of(2026, 10, 1), "Khám phá", null));

        assertThat(patchDay("""
                { "title": "Khám phá", "note": "" }
                """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true, "data": { "id": 11, "dayIndex": 1, "title": "Khám phá", "note": null } }
                        """);

        // "" must reach the service untouched: it means "clear", unlike an omitted field
        ArgumentCaptor<UpdateTripDayRequest> request = ArgumentCaptor.forClass(UpdateTripDayRequest.class);
        verify(tripDayService).update(eq(TRIP_ID), eq(DAY_ID), request.capture());
        assertThat(request.getValue()).isEqualTo(new UpdateTripDayRequest("Khám phá", ""));
    }

    @Test
    void updateReturns403WhenEditDeniedAndNeverReachesService() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(patchDay("""
                { "title": "Sửa trộm" }
                """))
                .hasStatus(HttpStatus.FORBIDDEN);
        verify(tripDayService, never()).update(any(), any(), any());
    }

    @Test
    void updateWithTooLongTitleReturns400WithFieldDetail() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(patchDay("""
                { "title": "%s" }
                """.formatted("a".repeat(161))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "title", "message": "Tiêu đề của ngày không được vượt quá 160 ký tự" } ] }
                        """);
        verify(tripDayService, never()).update(any(), any(), any());
    }

    @Test
    void updateReturns404WhenDayIsNotInTheTrip() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(tripDayService.update(eq(TRIP_ID), eq(DAY_ID), any()))
                .thenThrow(new ResourceNotFoundException("TripDay", DAY_ID));

        assertThat(patchDay("""
                { "title": "X" }
                """))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void listWithNonNumericTripIdReturns400() {
        assertThat(mvc.get().uri("/api/v1/trips/abc/days").header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
    }

    private MvcTestResult patchDay(String json) {
        return mvc.patch().uri(DAYS_URL + "/" + DAY_ID).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

}
