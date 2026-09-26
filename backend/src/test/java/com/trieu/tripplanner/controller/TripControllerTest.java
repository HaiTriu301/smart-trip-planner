package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.PageResponse;
import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.internal.TripFilter;
import com.trieu.tripplanner.dto.request.CreateTripRequest;
import com.trieu.tripplanner.dto.request.UpdateTripRequest;
import com.trieu.tripplanner.dto.response.TripResponse;
import com.trieu.tripplanner.dto.response.TripSummaryResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.model.enums.TripStatus;
import com.trieu.tripplanner.model.enums.TripVisibility;
import com.trieu.tripplanner.security.CustomUserDetails;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.security.permission.TripPermissionEvaluator;
import com.trieu.tripplanner.service.TripService;
import com.trieu.tripplanner.support.TestUsers;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Web layer only: TripService and the tripPermission bean are mocks, so these tests pin down routing,
 * validation, the error format and that every trip-scoped endpoint is guarded by @PreAuthorize.
 */
@WebMvcTest(TripController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class TripControllerTest {

    private static final String TRIPS_URL = "/api/v1/trips";
    private static final String TRIP_URL = "/api/v1/trips/5";
    private static final long USER_ID = 7L;
    private static final long TRIP_ID = 5L;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TripService tripService;

    // Same bean name the SpEL expressions reference: @tripPermission
    @MockitoBean(name = "tripPermission")
    private TripPermissionEvaluator tripPermission;

    private String bearer;

    @BeforeEach
    void signIn() {
        bearer = "Bearer " + jwtTokenProvider.generateAccessToken(TestUsers.verified(USER_ID, "an@example.com")).token();
    }

    // ---- GET /trips ------------------------------------------------------------------------------------------

    @Test
    void listPassesUserFromTokenFiltersAndDefaultPaging() {
        TripSummaryResponse row = new TripSummaryResponse(TRIP_ID, "Đà Lạt", "da-lat-x7k2qp", null, "Đà Lạt",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), TripStatus.PLANNED, TripVisibility.PRIVATE,
                Instant.parse("2026-09-26T10:00:00Z"));
        when(tripService.list(eq(USER_ID), any(), any())).thenReturn(new PageResponse<>(List.of(row), 0, 20, 1, 1, false));

        assertThat(mvc.get().uri(TRIPS_URL + "?status=PLANNED&q=lat&from=2026-10-01&to=2026-10-31")
                .header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "items": [ { "id": 5, "title": "Đà Lạt", "startDate": "2026-10-01", "status": "PLANNED" } ],
                                    "page": 0, "size": 20, "totalElements": 1, "hasNext": false } }
                        """);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(tripService).list(eq(USER_ID),
                eq(new TripFilter(TripStatus.PLANNED, "lat", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))),
                pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void listCapsPageSizeAt100() {
        when(tripService.list(eq(USER_ID), any(), any())).thenReturn(new PageResponse<>(List.of(), 0, 100, 0, 0, false));

        assertThat(mvc.get().uri(TRIPS_URL + "?size=500").header(HttpHeaders.AUTHORIZATION, bearer)).hasStatusOk();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(tripService).list(eq(USER_ID), any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void listWithoutTokenReturns401() {
        assertThat(mvc.get().uri(TRIPS_URL))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(tripService);
    }

    @Test
    void listWithUnknownStatusReturns400() {
        assertThat(mvc.get().uri(TRIPS_URL + "?status=FLYING").header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(tripService);
    }

    // ---- POST /trips -----------------------------------------------------------------------------------------

    @Test
    void createReturns201AndOwnerComesFromToken() {
        when(tripService.create(eq(USER_ID), any())).thenReturn(sampleTrip());

        assertThat(mvc.post().uri(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "Da Lat", "startDate": "2026-10-01", "endDate": "2026-10-03",
                          "budgetAmount": 5000000, "currency": "VND" }
                        """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true, "data": { "id": 5, "ownerId": 7, "slug": "da-lat-x7k2qp", "status": "DRAFT", "version": 0 } }
                        """);

        ArgumentCaptor<CreateTripRequest> request = ArgumentCaptor.forClass(CreateTripRequest.class);
        verify(tripService).create(eq(USER_ID), request.capture());
        assertThat(request.getValue().title()).isEqualTo("Da Lat");
        assertThat(request.getValue().budgetAmount()).isEqualByComparingTo("5000000");
    }

    @Test
    void createWithInvalidBodyReturns400WithFieldDetails() {
        assertThat(mvc.post().uri(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "  ", "currency": "vnd", "destinationLat": 91, "budgetAmount": -1 }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.details[*].field")
                .asArray()
                .contains("title", "startDate", "endDate", "currency", "destinationLat", "budgetAmount");
        verifyNoInteractions(tripService);
    }

    @Test
    void createRejectedByBusinessRuleReturns400OnEndDate() {
        when(tripService.create(eq(USER_ID), any())).thenThrow(BusinessRuleException.invalidField(
                "endDate", "error.trip.too-long", "Trip spans 61 days, max 60", 60));

        assertThat(mvc.post().uri(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "Xuyen Viet", "startDate": "2026-10-01", "endDate": "2026-11-30" }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "endDate", "message": "Chuyến đi dài tối đa 60 ngày" } ] }
                        """);
    }

    @Test
    void createWithoutTokenReturns401() {
        assertThat(mvc.post().uri(TRIPS_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(tripService);
    }

    // ---- GET /trips/{id} -------------------------------------------------------------------------------------

    @Test
    void getReturnsTripWhenViewAllowed() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(tripService.get(TRIP_ID)).thenReturn(sampleTrip());

        assertThat(mvc.get().uri(TRIP_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().extractingPath("$.data.title").isEqualTo("Đà Lạt 3 ngày");

        // The evaluator receives the principal built from the token, not something from the request
        verify(tripPermission).canView(eq(TRIP_ID),
                argThat(p -> p instanceof CustomUserDetails user && user.getId() == USER_ID));
    }

    @Test
    void getReturns403WhenViewDeniedAndNeverReachesService() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(mvc.get().uri(TRIP_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        verify(tripService, never()).get(any());
    }

    @Test
    void getReturns404WhenTripDoesNotExist() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(tripService.get(TRIP_ID)).thenThrow(new ResourceNotFoundException("Trip", TRIP_ID));

        assertThat(mvc.get().uri(TRIP_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void getWithNonNumericIdReturns400() {
        assertThat(mvc.get().uri(TRIPS_URL + "/abc").header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
    }

    // ---- PATCH /trips/{id} -----------------------------------------------------------------------------------

    @Test
    void updateReturnsTripWhenEditAllowed() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(tripService.update(eq(TRIP_ID), any())).thenReturn(sampleTrip());

        assertThat(mvc.patch().uri(TRIP_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "Da Lat moi", "visibility": "LINK" }
                        """))
                .hasStatusOk()
                .bodyJson().extractingPath("$.data.id").isEqualTo(5);

        ArgumentCaptor<UpdateTripRequest> request = ArgumentCaptor.forClass(UpdateTripRequest.class);
        verify(tripService).update(eq(TRIP_ID), request.capture());
        assertThat(request.getValue().title()).isEqualTo("Da Lat moi");
        assertThat(request.getValue().visibility()).isEqualTo(TripVisibility.LINK);
        assertThat(request.getValue().startDate()).isNull(); // omitted fields stay null → "keep current value"
    }

    @Test
    void updateReturns403WhenEditDenied() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(mvc.patch().uri(TRIP_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "Hack" }
                        """))
                .hasStatus(HttpStatus.FORBIDDEN);
        verify(tripService, never()).update(any(), any());
    }

    @Test
    void updateWithBlankTitleReturns400() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(mvc.patch().uri(TRIP_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "   " }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "title", "message": "Tên chuyến đi không được để trống" } ] }
                        """);
        verify(tripService, never()).update(any(), any());
    }

    // ---- DELETE /trips/{id} ----------------------------------------------------------------------------------

    @Test
    void deleteReturns200WithNullDataForOwner() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(mvc.delete().uri(TRIP_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true, "data": null }
                        """);
        verify(tripService).delete(TRIP_ID);
    }

    @Test
    void deleteReturns403ForNonOwner() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(mvc.delete().uri(TRIP_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.FORBIDDEN);
        verify(tripService, never()).delete(any());
    }

    private static TripResponse sampleTrip() {
        return new TripResponse(TRIP_ID, USER_ID, "Đà Lạt 3 ngày", "da-lat-x7k2qp", null, null, "Đà Lạt",
                null, null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), new BigDecimal("5000000.00"),
                "VND", TripStatus.DRAFT, TripVisibility.PRIVATE, 0L,
                Instant.parse("2026-09-26T10:00:00Z"), Instant.parse("2026-09-26T10:00:00Z"));
    }

}
