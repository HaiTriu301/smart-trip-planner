package com.trieu.tripplanner.controller;

import static com.trieu.tripplanner.support.TestComments.createRequest;
import static com.trieu.tripplanner.support.TestComments.onActivity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.response.CommentResponse;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.security.permission.TripPermissionEvaluator;
import com.trieu.tripplanner.service.CommentService;
import com.trieu.tripplanner.support.TestUsers;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * Web layer only: CommentService and the tripPermission bean are mocks.
 */
@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class CommentControllerTest {

    private static final long TRIP_ID = 5L;
    private static final long USER_ID = 8L;
    private static final String COMMENTS_URL = "/api/v1/trips/5/comments";
    private static final Instant CREATED_AT = Instant.parse("2026-10-17T03:00:00Z");

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CommentService commentService;

    // Same bean name the SpEL expressions reference: @tripPermission
    @MockitoBean(name = "tripPermission")
    private TripPermissionEvaluator tripPermission;

    private String bearer;

    @BeforeEach
    void signIn() {
        bearer = "Bearer " + jwtTokenProvider.generateAccessToken(TestUsers.verified(USER_ID, "viewer@example.com"))
                .token();
    }

    // ---- POST /trips/{tripId}/comments ----------------------------------------------------------------------------

    @Test
    void createReturns201WithTheComment() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(commentService.create(TRIP_ID, USER_ID, createRequest("Nhớ mang áo ấm")))
                .thenReturn(comment("Nhớ mang áo ấm"));

        assertThat(create("""
                { "content": "Nhớ mang áo ấm" }
                """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "id": 51, "activityId": null, "parentId": null,
                                    "author": { "id": 8, "fullName": "Người xem", "avatarUrl": null },
                                    "content": "Nhớ mang áo ấm", "createdAt": "2026-10-17T03:00:00Z" } }
                        """);
    }

    @Test
    void createTakesTheAuthorFromTheSignInNeverFromTheBody() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(commentService.create(eq(TRIP_ID), anyLong(), any())).thenReturn(comment("Xin chào"));

        // The request has no such field: the author is the signed-in user (CLAUDE.md rule 16)
        assertThat(create("""
                { "content": "Xin chào", "userId": 99, "authorId": 99 }
                """)).hasStatus(HttpStatus.CREATED);
        verify(commentService).create(TRIP_ID, USER_ID, createRequest("Xin chào"));
    }

    @Test
    void createPassesTheActivityIdOfTheBodyToTheService() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(commentService.create(eq(TRIP_ID), eq(USER_ID), any())).thenReturn(comment("Chợ mở lúc mấy giờ?"));

        assertThat(create("""
                { "content": "Chợ mở lúc mấy giờ?", "activityId": 31 }
                """)).hasStatus(HttpStatus.CREATED);
        verify(commentService).create(TRIP_ID, USER_ID, onActivity(createRequest("Chợ mở lúc mấy giờ?"), 31L));
    }

    @Test
    void createWithNonNumericActivityIdReturns400() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(create("""
                { "content": "Xin chào", "activityId": "abc" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(commentService);
    }

    @Test
    void createWithTwoThousandCharactersIsAccepted() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(commentService.create(eq(TRIP_ID), eq(USER_ID), any())).thenReturn(comment("a".repeat(2000)));

        assertThat(create("""
                { "content": "%s" }
                """.formatted("a".repeat(2000)))).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void createWithTwoThousandAndOneCharactersReturns400OnTheContentField() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(create("""
                { "content": "%s" }
                """.formatted("a".repeat(2001))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "content",
                                         "message": "Bình luận không được vượt quá 2000 ký tự" } ] }
                        """);
        verifyNoInteractions(commentService);
    }

    @Test
    void createWithEmptyContentReturns400OnTheContentField() {
        assertContentRefused("""
                { "content": "" }
                """);
    }

    @Test
    void createWithOnlySpacesAndLineBreaksReturns400OnTheContentField() {
        assertContentRefused("""
                { "content": "   \\n  " }
                """);
    }

    @Test
    void createWithoutContentReturns400OnTheContentField() {
        assertContentRefused("{}");
    }

    @Test
    void createWithoutBodyReturns400() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(mvc.post().uri(COMMENTS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(commentService);
    }

    @Test
    void createReturns403WhenTheCallerMayNotViewTheTripAndNeverReachesService() {
        // A stranger, or a PENDING / REMOVED member: the evaluator refuses them
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(create("""
                { "content": "Xin chào" }
                """))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        verify(commentService, never()).create(any(), any(), any());
    }

    @Test
    void createWithoutTokenReturns401() {
        assertThat(mvc.post().uri(COMMENTS_URL).contentType(MediaType.APPLICATION_JSON).content("""
                { "content": "Xin chào" }
                """)).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(commentService, tripPermission);
    }

    @Test
    void createForMissingTripReturns404() {
        // The evaluator lets a missing trip through so that the answer is 404, not 403 (CLAUDE.md rule 15)
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(commentService.create(eq(TRIP_ID), eq(USER_ID), any()))
                .thenThrow(new ResourceNotFoundException("Trip", TRIP_ID));

        assertThat(create("""
                { "content": "Xin chào" }
                """))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void createWithNonNumericTripIdReturns400() {
        assertThat(mvc.post().uri("/api/v1/trips/abc/comments").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        { "content": "Xin chào" }
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(commentService);
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private void assertContentRefused(String json) {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(create(json))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "content",
                                         "message": "Nội dung bình luận không được để trống" } ] }
                        """);
        verifyNoInteractions(commentService);
    }

    private MvcTestResult create(String json) {
        return mvc.post().uri(COMMENTS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    /** The one place of this class that builds the response, so a new field is added here only. */
    private static CommentResponse comment(String content) {
        return new CommentResponse(51L, null, null, new CommentResponse.Author(USER_ID, "Người xem", null), content,
                CREATED_AT);
    }

}
