package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.request.CreateActivityRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.FieldViolation;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.model.enums.ActivityType;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.security.permission.TripPermissionEvaluator;
import com.trieu.tripplanner.service.ActivityService;
import com.trieu.tripplanner.support.TestUsers;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
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
 * Web layer only: ActivityService and the tripPermission bean are mocks, so these tests pin down routing,
 * validation, the JSON format and that every endpoint is guarded by @PreAuthorize.
 */
@WebMvcTest(ActivityController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ActivityControllerTest {

    private static final long USER_ID = 7L;
    private static final long TRIP_ID = 5L;
    private static final long DAY_ID = 11L;
    private static final String DAY_ACTIVITIES_URL = "/api/v1/trips/5/days/11/activities";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private ActivityService activityService;

    // Same bean name the SpEL expressions reference: @tripPermission
    @MockitoBean(name = "tripPermission")
    private TripPermissionEvaluator tripPermission;

    private String bearer;

    @BeforeEach
    void signIn() {
        bearer = "Bearer " + jwtTokenProvider.generateAccessToken(TestUsers.verified(USER_ID, "an@example.com")).token();
    }

    // ---- GET /trips/{tripId}/days/{dayId}/activities ---------------------------------------------------------

    @Test
    void listReturnsActivitiesInOrderWhenViewAllowed() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        Instant now = Instant.parse("2026-09-29T10:00:00Z");
        when(activityService.list(TRIP_ID, DAY_ID)).thenReturn(List.of(
                sampleActivity(),
                new ActivityResponse(22L, DAY_ID, "Dạo hồ", ActivityType.OTHER, null, null, 2000, null, null, null,
                        null, USER_ID, 0L, now, now)));

        assertThat(mvc.get().uri(DAY_ACTIVITIES_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": [ { "id": 21, "title": "An trua", "startTime": "11:30", "endTime": "13:00",
                                      "orderIndex": 1000 },
                                    { "id": 22, "title": "Dạo hồ", "type": "OTHER", "startTime": null,
                                      "endTime": null, "orderIndex": 2000 } ] }
                        """);
    }

    @Test
    void listOfAnEmptyDayReturnsAnEmptyArray() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.list(TRIP_ID, DAY_ID)).thenReturn(List.of());

        assertThat(mvc.get().uri(DAY_ACTIVITIES_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true, "data": [] }
                        """);
    }

    @Test
    void listWithoutTokenReturns401() {
        assertThat(mvc.get().uri(DAY_ACTIVITIES_URL))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(activityService);
    }

    @Test
    void listReturns403WhenViewDeniedAndNeverReachesService() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(mvc.get().uri(DAY_ACTIVITIES_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        verifyNoInteractions(activityService);
    }

    @Test
    void listIsGuardedByViewPermissionNotEditPermission() {
        // A viewer (Phase 4) may read the itinerary without being allowed to change it
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(false);
        when(activityService.list(TRIP_ID, DAY_ID)).thenReturn(List.of());

        assertThat(mvc.get().uri(DAY_ACTIVITIES_URL).header(HttpHeaders.AUTHORIZATION, bearer)).hasStatusOk();
    }

    @Test
    void listReturns404WhenDayIsNotInTheTrip() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.list(TRIP_ID, DAY_ID)).thenThrow(new ResourceNotFoundException("TripDay", DAY_ID));

        assertThat(mvc.get().uri(DAY_ACTIVITIES_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ---- POST /trips/{tripId}/days/{dayId}/activities --------------------------------------------------------

    @Test
    void createReturns201AndCreatorComesFromToken() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.create(eq(TRIP_ID), eq(DAY_ID), eq(USER_ID), any(), eq(false))).thenReturn(sampleActivity());

        assertThat(post("""
                { "title": "An trua", "type": "FOOD", "startTime": "11:30", "endTime": "13:00",
                  "costAmount": 350000, "currency": "VND", "bookingUrl": "https://example.com/b/1" }
                """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "id": 21, "dayId": 11, "title": "An trua", "type": "FOOD",
                                    "startTime": "11:30", "endTime": "13:00", "orderIndex": 1000,
                                    "costAmount": 350000, "currency": "VND", "createdById": 7, "version": 0 } }
                        """);

        ArgumentCaptor<CreateActivityRequest> request = ArgumentCaptor.forClass(CreateActivityRequest.class);
        verify(activityService).create(eq(TRIP_ID), eq(DAY_ID), eq(USER_ID), request.capture(), eq(false));
        assertThat(request.getValue().title()).isEqualTo("An trua");
        assertThat(request.getValue().type()).isEqualTo(ActivityType.FOOD);
        assertThat(request.getValue().startTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(request.getValue().endTime()).isEqualTo(LocalTime.of(13, 0));
        assertThat(request.getValue().costAmount()).isEqualByComparingTo("350000");
    }

    @Test
    void createAcceptsTimesWithSeconds() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.create(eq(TRIP_ID), eq(DAY_ID), eq(USER_ID), any(), eq(false))).thenReturn(sampleActivity());

        assertThat(post("""
                { "title": "An trua", "startTime": "11:30:00" }
                """))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void createWithoutTokenReturns401() {
        assertThat(mvc.post().uri(DAY_ACTIVITIES_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(activityService);
    }

    @Test
    void createReturns403WhenEditDeniedAndNeverReachesService() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(post("""
                { "title": "Them trom" }
                """))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        verifyNoInteractions(activityService);
    }

    @Test
    void createWithInvalidBodyReturns400WithFieldDetails() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(post("""
                { "title": "  ", "costAmount": -1, "currency": "vnd", "bookingUrl": "ftp://example.com" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.details[*].field")
                .asArray()
                .containsExactlyInAnyOrder("title", "costAmount", "currency", "bookingUrl");
        verifyNoInteractions(activityService);
    }

    @Test
    void createWithTooLongTitleReturns400WithVietnameseMessage() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(post("""
                { "title": "%s" }
                """.formatted("a".repeat(201))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "title", "message": "Tên hoạt động không được vượt quá 200 ký tự" } ] }
                        """);
        verifyNoInteractions(activityService);
    }

    @Test
    void createWithUnknownTypeOrImpossibleTimeReturns400() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(post("""
                { "title": "Bay", "type": "FLYING" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        assertThat(post("""
                { "title": "Khuya", "startTime": "25:00" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(activityService);
    }

    @Test
    void createRejectedByTimeRuleReturns400OnEndTime() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.create(eq(TRIP_ID), eq(DAY_ID), eq(USER_ID), any(), eq(false)))
                .thenThrow(BusinessRuleException.invalidField("endTime", "error.activity.end-not-after-start",
                        "Activity end time 09:00 is not after start time 10:00"));

        assertThat(post("""
                { "title": "Sai gio", "startTime": "10:00", "endTime": "09:00" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "endTime", "message": "Giờ kết thúc phải sau giờ bắt đầu" } ] }
                        """);
    }

    @Test
    void createReturns409WhenTimesOverlapAndNamesTheOtherActivity() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.create(eq(TRIP_ID), eq(DAY_ID), eq(USER_ID), any(), eq(false)))
                .thenThrow(new BusinessRuleException(ErrorCode.ACTIVITY_TIME_CONFLICT,
                        "Activity 09:30-10:30 overlaps 1 activities of day 11, first is activity 31",
                        List.of(FieldViolation.of("startTime", "error.activity.time-conflict-with",
                                "Ăn sáng", "09:00", "10:00"))));

        MvcTestResult result = post("""
                { "title": "Ca phe", "startTime": "09:30", "endTime": "10:30" }
                """);

        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "ACTIVITY_TIME_CONFLICT",
                          "message": "Hoạt động bị trùng giờ với một hoạt động khác trong ngày",
                          "details": [ { "field": "startTime" } ] }
                        """);
        assertThat(result).bodyJson().extractingPath("$.details[0].message")
                .isEqualTo("Trùng giờ với hoạt động \"Ăn sáng\" (09:00 - 10:00)");
    }

    @Test
    void createWithAllowOverlapPassesTheFlagToTheService() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.create(eq(TRIP_ID), eq(DAY_ID), eq(USER_ID), any(), eq(true)))
                .thenReturn(sampleActivity());

        assertThat(mvc.post().uri(DAY_ACTIVITIES_URL + "?allowOverlap=true")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "Ca phe", "startTime": "09:30", "endTime": "10:30" }
                        """))
                .hasStatus(HttpStatus.CREATED);
        verify(activityService).create(eq(TRIP_ID), eq(DAY_ID), eq(USER_ID), any(), eq(true));
    }

    @Test
    void createWithAllowOverlapThatIsNotABooleanReturns400() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(mvc.post().uri(DAY_ACTIVITIES_URL + "?allowOverlap=maybe")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "Ca phe" }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(activityService);
    }

    @Test
    void createReturns404WhenDayIsNotInTheTrip() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.create(eq(TRIP_ID), eq(DAY_ID), eq(USER_ID), any(), eq(false)))
                .thenThrow(new ResourceNotFoundException("TripDay", DAY_ID));

        assertThat(post("""
                { "title": "Nham ngay" }
                """))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void createWithNonNumericDayIdReturns400() {
        assertThat(mvc.post().uri("/api/v1/trips/5/days/abc/activities").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "X" }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(activityService);
    }

    private MvcTestResult post(String json) {
        return mvc.post().uri(DAY_ACTIVITIES_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private static ActivityResponse sampleActivity() {
        Instant now = Instant.parse("2026-09-29T10:00:00Z");
        return new ActivityResponse(21L, DAY_ID, "An trua", ActivityType.FOOD, LocalTime.of(11, 30),
                LocalTime.of(13, 0), 1000, null, new BigDecimal("350000.00"), "VND", "https://example.com/b/1",
                USER_ID, 0L, now, now);
    }

}
