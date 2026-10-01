package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.request.CreateActivityRequest;
import com.trieu.tripplanner.dto.request.ReorderActivitiesRequest;
import com.trieu.tripplanner.dto.request.UpdateActivityRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.dto.response.PlaceResponse;
import com.trieu.tripplanner.dto.response.TripDayDetailResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.FieldViolation;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.model.enums.ActivityType;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.security.permission.TripPermissionEvaluator;
import com.trieu.tripplanner.service.ActivityService;
import com.trieu.tripplanner.support.TestActivities;
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
    private static final long ACTIVITY_ID = 21L;
    private static final String DAY_ACTIVITIES_URL = "/api/v1/trips/5/days/11/activities";
    private static final String ACTIVITY_URL = "/api/v1/trips/5/activities/21";
    private static final String REORDER_URL = "/api/v1/trips/5/activities/reorder";

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
                TestActivities.response(22L, DAY_ID, "Dạo hồ", ActivityType.OTHER, null, null, 2000, null, null, null,
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
    void activityWithAPlaceShowsItInFullAndActivityWithoutOneShowsNull() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        Instant now = Instant.parse("2026-09-29T10:00:00Z");
        when(activityService.list(TRIP_ID, DAY_ID)).thenReturn(List.of(
                TestActivities.withPlace(sampleActivity(), new PlaceResponse(71L, PlaceProvider.MOCK, "Chợ Đà Lạt",
                        "Nguyễn Thị Minh Khai, Đà Lạt", new BigDecimal("11.9434358"), new BigDecimal("108.4371779"),
                        "SHOPPING")),
                TestActivities.response(22L, DAY_ID, "Dạo hồ", ActivityType.OTHER, null, null, 2000, null, null, null,
                        null, USER_ID, 0L, now, now)));

        assertThat(mvc.get().uri(DAY_ACTIVITIES_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": [ { "id": 21,
                                      "place": { "id": 71, "provider": "MOCK", "name": "Chợ Đà Lạt",
                                                 "address": "Nguyễn Thị Minh Khai, Đà Lạt", "lat": 11.9434358,
                                                 "lng": 108.4371779, "category": "SHOPPING" } },
                                    { "id": 22, "place": null } ] }
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
    void createPassesThePlaceIdOnAndShowsThePlaceOfTheNewActivity() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.create(eq(TRIP_ID), eq(DAY_ID), eq(USER_ID), any(), eq(false)))
                .thenReturn(TestActivities.withPlace(sampleActivity(), new PlaceResponse(71L, PlaceProvider.MOCK,
                        "Chợ Đà Lạt", null, new BigDecimal("11.9434358"), new BigDecimal("108.4371779"), "SHOPPING")));

        assertThat(post("""
                { "title": "An trua", "placeId": 71 }
                """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "id": 21, "place": { "id": 71, "name": "Chợ Đà Lạt" } } }
                        """);

        ArgumentCaptor<CreateActivityRequest> request = ArgumentCaptor.forClass(CreateActivityRequest.class);
        verify(activityService).create(eq(TRIP_ID), eq(DAY_ID), eq(USER_ID), request.capture(), eq(false));
        assertThat(request.getValue().placeId()).isEqualTo(71L);
    }

    @Test
    void createWithAPlaceThatCannotBeUsedIsAValidationErrorOnPlaceId() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.create(eq(TRIP_ID), eq(DAY_ID), eq(USER_ID), any(), eq(false)))
                .thenThrow(BusinessRuleException.invalidField("placeId", "error.activity.place-not-found",
                        "Place 999 cannot be attached by user 7"));

        assertThat(post("""
                { "title": "An trua", "placeId": 999 }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "placeId", "message": "Địa điểm không tồn tại" } ] }
                        """);
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
    void createAcceptsNoteOfExactly255Characters() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.create(eq(TRIP_ID), eq(DAY_ID), eq(USER_ID), any(), eq(false))).thenReturn(sampleActivity());

        // Vietnamese letters: the limit counts characters, not bytes
        assertThat(post("""
                { "title": "An trua", "note": "%s" }
                """.formatted("ă".repeat(255))))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void createWithNoteOver255CharactersReturns400() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(post("""
                { "title": "An trua", "note": "%s" }
                """.formatted("a".repeat(256))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "note", "message": "Ghi chú của hoạt động không được vượt quá 255 ký tự" } ] }
                        """);
        verifyNoInteractions(activityService);
    }

    @Test
    void updateWithNoteOver255CharactersReturns400() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(patch(ACTIVITY_URL, """
                { "note": "%s" }
                """.formatted("a".repeat(256))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.details[0].field").isEqualTo("note");
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

    // ---- PATCH /trips/{tripId}/activities/{activityId} ------------------------------------------------------

    @Test
    void updatePassesThePlaceIdWithTheUserOfTheTokenAndShowsTheNewPlace() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.update(eq(TRIP_ID), eq(ACTIVITY_ID), eq(USER_ID), any(), eq(false)))
                .thenReturn(TestActivities.withPlace(sampleActivity(), new PlaceResponse(72L, PlaceProvider.MOCK,
                        "Bảo tàng Lâm Đồng", null, new BigDecimal("11.9416000"), new BigDecimal("108.4583000"), null)));

        // userId in the body is ignored: the place is checked against the user inside the token (id 7)
        assertThat(patch(ACTIVITY_URL, """
                { "placeId": 72, "userId": 99 }
                """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "id": 21, "place": { "id": 72, "name": "Bảo tàng Lâm Đồng" } } }
                        """);

        ArgumentCaptor<UpdateActivityRequest> request = ArgumentCaptor.forClass(UpdateActivityRequest.class);
        verify(activityService).update(eq(TRIP_ID), eq(ACTIVITY_ID), eq(USER_ID), request.capture(), eq(false));
        assertThat(request.getValue().placeId()).isEqualTo(72L);
    }

    @Test
    void updateReturnsTheActivityWhenEditAllowed() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.update(eq(TRIP_ID), eq(ACTIVITY_ID), eq(USER_ID), any(), eq(false))).thenReturn(sampleActivity());

        assertThat(patch(ACTIVITY_URL, """
                { "title": "An trua", "endTime": "13:00", "note": "", "bookingUrl": "" }
                """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "id": 21, "dayId": 11, "title": "An trua", "startTime": "11:30",
                                    "endTime": "13:00", "orderIndex": 1000, "version": 0 } }
                        """);

        // "" must reach the service untouched: it means "clear", unlike an omitted field
        ArgumentCaptor<UpdateActivityRequest> request = ArgumentCaptor.forClass(UpdateActivityRequest.class);
        verify(activityService).update(eq(TRIP_ID), eq(ACTIVITY_ID), eq(USER_ID), request.capture(), eq(false));
        assertThat(request.getValue()).isEqualTo(
                TestActivities.updateRequest("An trua", null, null, LocalTime.of(13, 0), "", null, null, ""));
    }

    @Test
    void updateWithEmptyBodyObjectIsAcceptedAsNoChange() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.update(eq(TRIP_ID), eq(ACTIVITY_ID), eq(USER_ID), any(), eq(false))).thenReturn(sampleActivity());

        assertThat(patch(ACTIVITY_URL, "{}")).hasStatusOk();
    }

    @Test
    void updateWithoutTokenReturns401() {
        assertThat(mvc.patch().uri(ACTIVITY_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(activityService);
    }

    @Test
    void updateReturns403WhenEditDeniedAndNeverReachesService() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(patch(ACTIVITY_URL, """
                { "title": "Sua trom" }
                """))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        verifyNoInteractions(activityService);
    }

    @Test
    void updateWithInvalidBodyReturns400WithFieldDetails() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(patch(ACTIVITY_URL, """
                { "title": "   ", "costAmount": -1, "currency": "vnd", "bookingUrl": "ftp://example.com" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.details[*].field")
                .asArray()
                .containsExactlyInAnyOrder("title", "costAmount", "currency", "bookingUrl");
        verifyNoInteractions(activityService);
    }

    @Test
    void updateReturns409WhenNewTimesOverlap() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.update(eq(TRIP_ID), eq(ACTIVITY_ID), eq(USER_ID), any(), eq(false)))
                .thenThrow(new BusinessRuleException(ErrorCode.ACTIVITY_TIME_CONFLICT,
                        "Activity 09:30-10:30 overlaps 1 activities of day 11, first is activity 32",
                        List.of(FieldViolation.of("startTime", "error.activity.time-conflict-with",
                                "Cà phê", "10:00", "11:00"))));

        assertThat(patch(ACTIVITY_URL, """
                { "startTime": "09:30", "endTime": "10:30" }
                """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "ACTIVITY_TIME_CONFLICT",
                          "details": [ { "field": "startTime" } ] }
                        """);
    }

    @Test
    void updateWithAllowOverlapPassesTheFlagToTheService() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.update(eq(TRIP_ID), eq(ACTIVITY_ID), eq(USER_ID), any(), eq(true))).thenReturn(sampleActivity());

        assertThat(patch(ACTIVITY_URL + "?allowOverlap=true", """
                { "startTime": "09:30", "endTime": "10:30" }
                """))
                .hasStatusOk();
        verify(activityService).update(eq(TRIP_ID), eq(ACTIVITY_ID), eq(USER_ID), any(), eq(true));
    }

    @Test
    void updateReturns404WhenActivityIsNotInTheTrip() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.update(eq(TRIP_ID), eq(ACTIVITY_ID), eq(USER_ID), any(), eq(false)))
                .thenThrow(new ResourceNotFoundException("Activity", ACTIVITY_ID));

        assertThat(patch(ACTIVITY_URL, """
                { "title": "Nham chuyen" }
                """))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void updateWithNonNumericActivityIdReturns400() {
        assertThat(patch("/api/v1/trips/5/activities/abc", """
                { "title": "X" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(activityService);
    }

    // ---- DELETE /trips/{tripId}/activities/{activityId} -----------------------------------------------------

    @Test
    void deleteReturns200WithNullDataWhenEditAllowed() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(mvc.delete().uri(ACTIVITY_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true, "data": null }
                        """);
        verify(activityService).delete(TRIP_ID, ACTIVITY_ID);
    }

    @Test
    void deleteWithoutTokenReturns401() {
        assertThat(mvc.delete().uri(ACTIVITY_URL))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(activityService);
    }

    @Test
    void deleteReturns403WhenEditDeniedAndNeverReachesService() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(mvc.delete().uri(ACTIVITY_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        verifyNoInteractions(activityService);
    }

    @Test
    void deleteIsGuardedByEditPermissionNotViewPermission() {
        // A viewer (Phase 4) may read the itinerary but must not remove anything from it
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(mvc.delete().uri(ACTIVITY_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(activityService);
    }

    @Test
    void deleteReturns404WhenActivityIsNotInTheTrip() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        doThrow(new ResourceNotFoundException("Activity", ACTIVITY_ID))
                .when(activityService).delete(TRIP_ID, ACTIVITY_ID);

        assertThat(mvc.delete().uri(ACTIVITY_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void deleteWithNonNumericActivityIdReturns400() {
        assertThat(mvc.delete().uri("/api/v1/trips/5/activities/abc").header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(activityService);
    }

    // ---- PUT /trips/{tripId}/activities/reorder --------------------------------------------------------------

    @Test
    void reorderReturnsTheAffectedDaysWhenEditAllowed() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.reorder(eq(TRIP_ID), any(), eq(false))).thenReturn(List.of(
                new TripDayDetailResponse(DAY_ID, 1, LocalDate.of(2026, 10, 1), null, null, List.of(sampleActivity())),
                new TripDayDetailResponse(12L, 2, LocalDate.of(2026, 10, 2), null, null, List.of())));

        assertThat(put("""
                { "items": [ { "activityId": 21, "dayId": 11, "orderIndex": 1500 },
                             { "activityId": 22, "dayId": 12, "orderIndex": 1000 } ] }
                """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": [ { "id": 11, "dayIndex": 1, "date": "2026-10-01",
                                      "activities": [ { "id": 21, "title": "An trua", "orderIndex": 1000 } ] },
                                    { "id": 12, "dayIndex": 2, "activities": [] } ] }
                        """);

        ArgumentCaptor<ReorderActivitiesRequest> request = ArgumentCaptor.forClass(ReorderActivitiesRequest.class);
        verify(activityService).reorder(eq(TRIP_ID), request.capture(), eq(false));
        assertThat(request.getValue().items()).containsExactly(
                new ReorderActivitiesRequest.Item(21L, 11L, 1500),
                new ReorderActivitiesRequest.Item(22L, 12L, 1000));
    }

    @Test
    void reorderWithoutTokenReturns401() {
        assertThat(mvc.put().uri(REORDER_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(activityService);
    }

    @Test
    void reorderReturns403WhenEditDeniedAndNeverReachesService() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(put("""
                { "items": [ { "activityId": 21, "dayId": 11, "orderIndex": 1500 } ] }
                """))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        verifyNoInteractions(activityService);
    }

    @Test
    void reorderWithEmptyOrMissingItemsReturns400() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(put("""
                { "items": [] }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "items",
                                         "message": "Danh sách hoạt động cần sắp xếp không được để trống" } ] }
                        """);
        assertThat(put("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.details[0].field").isEqualTo("items");
        verifyNoInteractions(activityService);
    }

    @Test
    void reorderWithInvalidItemsReturns400NamingEachItemAndField() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(put("""
                { "items": [ { "activityId": 21, "dayId": 11, "orderIndex": 0 },
                             { "dayId": 11, "orderIndex": 1000 },
                             { "activityId": 23, "orderIndex": 1000000001 } ] }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.details[*].field")
                .asArray()
                .containsExactlyInAnyOrder("items[0].orderIndex", "items[1].activityId", "items[2].dayId",
                        "items[2].orderIndex");
        verifyNoInteractions(activityService);
    }

    @Test
    void reorderAcceptsTheLowestAndHighestOrderIndex() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.reorder(eq(TRIP_ID), any(), eq(false))).thenReturn(List.of());

        assertThat(put("""
                { "items": [ { "activityId": 21, "dayId": 11, "orderIndex": 1 },
                             { "activityId": 22, "dayId": 11, "orderIndex": 1000000000 } ] }
                """))
                .hasStatusOk();
    }

    @Test
    void reorderWithMoreThan200ItemsReturns400() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(put(itemsJson(201)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "details": [ { "field": "items",
                                         "message": "Mỗi lần chỉ sắp xếp được tối đa 200 hoạt động" } ] }
                        """);
        verifyNoInteractions(activityService);
    }

    @Test
    void reorderWithExactly200ItemsIsAccepted() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.reorder(eq(TRIP_ID), any(), eq(false))).thenReturn(List.of());

        assertThat(put(itemsJson(200))).hasStatusOk();
    }

    @Test
    void reorderWithABareArrayReturns400() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        // The body is an object holding the list (design.md 10.2 "Quy ước Reorder"), not the list itself
        assertThat(put("""
                [ { "activityId": 21, "dayId": 11, "orderIndex": 1500 } ]
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(activityService);
    }

    @Test
    void reorderReturns404WhenAnActivityIsNotInTheTrip() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.reorder(eq(TRIP_ID), any(), eq(false))).thenThrow(new ResourceNotFoundException("Activity", 99L));

        assertThat(put("""
                { "items": [ { "activityId": 99, "dayId": 11, "orderIndex": 1500 } ] }
                """))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void reorderRejectedForADuplicateActivityReturns400OnItems() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.reorder(eq(TRIP_ID), any(), eq(false)))
                .thenThrow(BusinessRuleException.invalidField("items", "error.reorder.duplicate-activity",
                        "Activity 21 appears more than once in the reorder request", "21"));

        assertThat(put("""
                { "items": [ { "activityId": 21, "dayId": 11, "orderIndex": 500 },
                             { "activityId": 21, "dayId": 12, "orderIndex": 900 } ] }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "items",
                                         "message": "Hoạt động 21 xuất hiện nhiều lần trong danh sách" } ] }
                        """);
    }

    @Test
    void reorderReturns409WhenAMovedActivityOverlapsInItsNewDay() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.reorder(eq(TRIP_ID), any(), eq(false)))
                .thenThrow(new BusinessRuleException(ErrorCode.ACTIVITY_TIME_CONFLICT,
                        "1 of 1 moved activities would overlap another activity in their new day",
                        List.of(FieldViolation.of("items[1].dayId", "error.reorder.time-conflict",
                                "Cà phê", "Ăn trưa", "11:30", "13:00"))));

        MvcTestResult result = put("""
                { "items": [ { "activityId": 21, "dayId": 11, "orderIndex": 500 },
                             { "activityId": 22, "dayId": 11, "orderIndex": 1500 } ] }
                """);

        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "ACTIVITY_TIME_CONFLICT",
                          "message": "Hoạt động bị trùng giờ với một hoạt động khác trong ngày",
                          "details": [ { "field": "items[1].dayId" } ] }
                        """);
        assertThat(result).bodyJson().extractingPath("$.details[0].message").isEqualTo(
                "Hoạt động \"Cà phê\" trùng giờ với hoạt động \"Ăn trưa\" (11:30 - 13:00) trong ngày mới");
    }

    @Test
    void reorderWithAllowOverlapPassesTheFlagToTheService() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);
        when(activityService.reorder(eq(TRIP_ID), any(), eq(true))).thenReturn(List.of());

        assertThat(mvc.put().uri(REORDER_URL + "?allowOverlap=true").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "items": [ { "activityId": 22, "dayId": 11, "orderIndex": 1500 } ] }
                        """))
                .hasStatusOk();
        verify(activityService).reorder(eq(TRIP_ID), any(), eq(true));
    }

    @Test
    void reorderWithAllowOverlapThatIsNotABooleanReturns400() {
        when(tripPermission.canEdit(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(mvc.put().uri(REORDER_URL + "?allowOverlap=maybe").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "items": [ { "activityId": 22, "dayId": 11, "orderIndex": 1500 } ] }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(activityService);
    }

    private MvcTestResult put(String json) {
        return mvc.put().uri(REORDER_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private static String itemsJson(int count) {
        StringBuilder items = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            items.append(i == 1 ? "" : ",")
                    .append("{\"activityId\":").append(i).append(",\"dayId\":11,\"orderIndex\":").append(i * 1000)
                    .append('}');
        }
        return "{\"items\":[" + items + "]}";
    }

    private MvcTestResult patch(String url, String json) {
        return mvc.patch().uri(url).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private MvcTestResult post(String json) {
        return mvc.post().uri(DAY_ACTIVITIES_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private static ActivityResponse sampleActivity() {
        Instant now = Instant.parse("2026-09-29T10:00:00Z");
        return TestActivities.response(21L, DAY_ID, "An trua", ActivityType.FOOD, LocalTime.of(11, 30),
                LocalTime.of(13, 0), 1000, null, new BigDecimal("350000.00"), "VND", "https://example.com/b/1",
                USER_ID, 0L, now, now);
    }

}
