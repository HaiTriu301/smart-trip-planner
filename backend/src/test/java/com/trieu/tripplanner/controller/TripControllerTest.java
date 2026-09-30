package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.PageResponse;
import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.internal.TripFilter;
import com.trieu.tripplanner.dto.request.CreateTripRequest;
import com.trieu.tripplanner.dto.request.UpdateTripRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.dto.response.TripDayDetailResponse;
import com.trieu.tripplanner.dto.response.TripDetailResponse;
import com.trieu.tripplanner.dto.response.TripResponse;
import com.trieu.tripplanner.dto.response.TripSummaryResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.FieldViolation;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.model.enums.ActivityType;
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
import java.time.LocalTime;
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
                Instant.parse("2026-09-26T10:00:00Z"), 12);
        when(tripService.list(eq(USER_ID), any(), any())).thenReturn(new PageResponse<>(List.of(row), 0, 20, 1, 1, false));

        assertThat(mvc.get().uri(TRIPS_URL + "?status=PLANNED&q=lat&from=2026-10-01&to=2026-10-31")
                .header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "items": [ { "id": 5, "title": "Đà Lạt", "startDate": "2026-10-01", "status": "PLANNED",
                                                 "activityCount": 12 } ],
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
        when(tripService.get(TRIP_ID)).thenReturn(sampleDetail());

        assertThat(mvc.get().uri(TRIP_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "id": 5, "title": "Đà Lạt 3 ngày", "slug": "da-lat-x7k2qp",
                                    "days": [ { "id": 11, "dayIndex": 1, "date": "2026-10-01", "title": "Đến nơi",
                                                "activities": [
                                                  { "id": 21, "dayId": 11, "title": "Ăn sáng", "type": "FOOD",
                                                    "startTime": "09:00", "endTime": "10:00", "orderIndex": 1000 },
                                                  { "id": 22, "dayId": 11, "title": "Dạo hồ", "startTime": null,
                                                    "orderIndex": 2000 } ] },
                                              { "id": 12, "dayIndex": 2, "date": "2026-10-02", "title": null,
                                                "activities": [] } ] } }
                        """);

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
        when(tripService.update(eq(TRIP_ID), any(), eq(false))).thenReturn(sampleTrip());

        assertThat(mvc.patch().uri(TRIP_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "Da Lat moi", "visibility": "LINK" }
                        """))
                .hasStatusOk()
                .bodyJson().extractingPath("$.data.id").isEqualTo(5);

        ArgumentCaptor<UpdateTripRequest> request = ArgumentCaptor.forClass(UpdateTripRequest.class);
        verify(tripService).update(eq(TRIP_ID), request.capture(), eq(false));
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
        verify(tripService, never()).update(any(), any(), anyBoolean());
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
        verify(tripService, never()).update(any(), any(), anyBoolean());
    }

    @Test
    void updateReturns409WhenDatesCutDaysThatHoldActivities() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(tripService.update(eq(TRIP_ID), any(), eq(false)))
                .thenThrow(new BusinessRuleException(ErrorCode.TRIP_DAY_HAS_ACTIVITIES,
                        "New range 2026-10-01..2026-10-02 of trip 5 drops 1 days holding 2 activities",
                        List.of(FieldViolation.of("force", "error.trip.dropped-activities", "2", "1"))));

        assertThat(mvc.patch().uri(TRIP_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "endDate": "2026-10-02" }
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "TRIP_DAY_HAS_ACTIVITIES",
                          "message": "Đổi ngày sẽ xoá những ngày đang có hoạt động",
                          "details": [ { "field": "force",
                                         "message": "2 hoạt động trong 1 ngày sẽ bị xoá nếu đổi ngày" } ] }
                        """);
    }

    @Test
    void updateWithForcePassesTheFlagToTheService() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(tripService.update(eq(TRIP_ID), any(), eq(true))).thenReturn(sampleTrip());

        assertThat(mvc.patch().uri(TRIP_URL + "?force=true").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "endDate": "2026-10-02" }
                        """))
                .hasStatusOk();
        verify(tripService).update(eq(TRIP_ID), any(), eq(true));
    }

    @Test
    void updateWithForceThatIsNotABooleanReturns400() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(mvc.patch().uri(TRIP_URL + "?force=yes-please").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "endDate": "2026-10-02" }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verify(tripService, never()).update(any(), any(), anyBoolean());
    }

    // ---- PATCH /trips/{id}/status ----------------------------------------------------------------------------

    @Test
    void updateStatusPassesTheNewStatusWhenEditAllowed() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(tripService.updateStatus(TRIP_ID, TripStatus.PLANNED)).thenReturn(sampleTrip());

        assertThat(mvc.patch().uri(TRIP_URL + "/status").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "status": "PLANNED" }
                        """))
                .hasStatusOk()
                .bodyJson().extractingPath("$.data.id").isEqualTo(5);
        verify(tripService).updateStatus(TRIP_ID, TripStatus.PLANNED);
    }

    @Test
    void updateStatusReturns403WhenEditDenied() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(mvc.patch().uri(TRIP_URL + "/status").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "status": "ARCHIVED" }
                        """))
                .hasStatus(HttpStatus.FORBIDDEN);
        verify(tripService, never()).updateStatus(any(), any());
    }

    @Test
    void updateStatusWithoutStatusReturns400OnTheStatusField() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(mvc.patch().uri(TRIP_URL + "/status").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "status", "message": "Trạng thái không được để trống" } ] }
                        """);
        verify(tripService, never()).updateStatus(any(), any());
    }

    @Test
    void updateStatusWithUnknownValueReturns400() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(mvc.patch().uri(TRIP_URL + "/status").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "status": "CANCELLED" }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verify(tripService, never()).updateStatus(any(), any());
    }

    // ---- description length (1000) ---------------------------------------------------------------------------

    @Test
    void createAcceptsDescriptionOfExactly1000Characters() {
        when(tripService.create(eq(USER_ID), any())).thenReturn(sampleTrip());

        // Vietnamese letters: the limit counts characters, not bytes
        assertThat(mvc.post().uri(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "Da Lat", "startDate": "2026-10-01", "endDate": "2026-10-03", "description": "%s" }
                        """.formatted("ă".repeat(1000))))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void createWithDescriptionOver1000CharactersReturns400() {
        assertThat(mvc.post().uri(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "Da Lat", "startDate": "2026-10-01", "endDate": "2026-10-03", "description": "%s" }
                        """.formatted("a".repeat(1001))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "description", "message": "Mô tả không được vượt quá 1000 ký tự" } ] }
                        """);
        verify(tripService, never()).create(any(), any());
    }

    @Test
    void updateWithDescriptionOver1000CharactersReturns400() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(mvc.patch().uri(TRIP_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "description": "%s" }
                        """.formatted("a".repeat(1001))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.details[0].field").isEqualTo("description");
        verify(tripService, never()).update(any(), any(), anyBoolean());
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

    private static TripDetailResponse sampleDetail() {
        return new TripDetailResponse(TRIP_ID, USER_ID, "Đà Lạt 3 ngày", "da-lat-x7k2qp", null, null, "Đà Lạt",
                null, null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), null,
                "VND", TripStatus.DRAFT, TripVisibility.PRIVATE, 0L,
                Instant.parse("2026-09-26T10:00:00Z"), Instant.parse("2026-09-26T10:00:00Z"),
                List.of(new TripDayDetailResponse(11L, 1, LocalDate.of(2026, 10, 1), "Đến nơi", null, List.of(
                                activity(21L, "Ăn sáng", ActivityType.FOOD, LocalTime.of(9, 0), LocalTime.of(10, 0), 1000),
                                activity(22L, "Dạo hồ", ActivityType.OTHER, null, null, 2000))),
                        new TripDayDetailResponse(12L, 2, LocalDate.of(2026, 10, 2), null, null, List.of())));
    }

    private static ActivityResponse activity(long id, String title, ActivityType type, LocalTime start, LocalTime end,
                                             int orderIndex) {
        Instant now = Instant.parse("2026-09-29T10:00:00Z");
        return new ActivityResponse(id, 11L, title, type, start, end, orderIndex, null, null, null, null, USER_ID, 0L,
                now, now);
    }

    private static TripResponse sampleTrip() {
        return new TripResponse(TRIP_ID, USER_ID, "Đà Lạt 3 ngày", "da-lat-x7k2qp", null, null, "Đà Lạt",
                null, null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3), new BigDecimal("5000000.00"),
                "VND", TripStatus.DRAFT, TripVisibility.PRIVATE, 0L,
                Instant.parse("2026-09-26T10:00:00Z"), Instant.parse("2026-09-26T10:00:00Z"));
    }

}
