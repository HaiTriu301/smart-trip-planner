package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.request.CreateShareLinkRequest;
import com.trieu.tripplanner.dto.response.ShareLinkResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.model.enums.SharePermission;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.security.permission.TripPermissionEvaluator;
import com.trieu.tripplanner.service.SharingService;
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
 * Web layer only: SharingService and the tripPermission bean are mocks.
 */
@WebMvcTest(ShareLinkController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class ShareLinkControllerTest {

    private static final long TRIP_ID = 5L;
    private static final String LINKS_URL = "/api/v1/trips/5/share-links";
    private static final String TOKEN = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
    private static final Instant CREATED_AT = Instant.parse("2026-10-13T03:00:00Z");
    private static final Instant NOV_1 = Instant.parse("2026-11-01T16:59:59Z");

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private SharingService sharingService;

    // Same bean name the SpEL expressions reference: @tripPermission
    @MockitoBean(name = "tripPermission")
    private TripPermissionEvaluator tripPermission;

    private String bearer;

    @BeforeEach
    void signIn() {
        bearer = "Bearer " + jwtTokenProvider.generateAccessToken(TestUsers.verified(7L, "owner@example.com")).token();
    }

    // ---- POST /trips/{tripId}/share-links ------------------------------------------------------------------------

    @Test
    void createReturns201WithALinkThatNeverExpiresWhenOwnerSendsNoBody() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);
        when(sharingService.createLink(TRIP_ID, null)).thenReturn(link(null));

        // No body and no Content-Type: the service receives null
        assertThat(mvc.post().uri(LINKS_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "id": 41, "token": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6", "permission": "VIEW",
                                    "expiresAt": null, "viewCount": 0, "createdAt": "2026-10-13T03:00:00Z" } }
                        """);
    }

    @Test
    void createWithAnExpiryHandsItToTheServiceAndReturnsIt() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);
        when(sharingService.createLink(TRIP_ID, new CreateShareLinkRequest(NOV_1))).thenReturn(link(NOV_1));

        assertThat(create("""
                { "expiresAt": "2026-11-01T16:59:59Z" }
                """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.data.expiresAt").isEqualTo("2026-11-01T16:59:59Z");
    }

    @Test
    void createWithAnEmptyJsonObjectIsALinkWithoutExpiry() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);
        when(sharingService.createLink(TRIP_ID, new CreateShareLinkRequest(null))).thenReturn(link(null));

        assertThat(create("{}"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.data.expiresAt").isNull();
    }

    @Test
    void createIgnoresAPermissionSentByTheClient() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);
        when(sharingService.createLink(TRIP_ID, new CreateShareLinkRequest(null))).thenReturn(link(null));

        // The request has no such field: a link is a view link whatever the client asks for (rule 14.26)
        assertThat(create("""
                { "permission": "EDIT" }
                """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.data.permission").isEqualTo("VIEW");
    }

    @Test
    void createWithAnExpiryThatIsADateWithoutTimeReturns400() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);

        // Not an instant: refused while the body is read, before the service
        assertThat(create("""
                { "expiresAt": "2026-11-01" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(sharingService);
    }

    @Test
    void createWithAnExpiryThatIsNotInTheFutureReturns400OnTheExpiresAtField() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);
        when(sharingService.createLink(eq(TRIP_ID), any())).thenThrow(BusinessRuleException.invalidField(
                "expiresAt", "error.share-link.expires-not-future", "share link expiry is not in the future"));

        assertThat(create("""
                { "expiresAt": "2020-01-01T00:00:00Z" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "expiresAt",
                                         "message": "Thời điểm hết hạn của liên kết phải ở tương lai" } ] }
                        """);
    }

    @Test
    void createReturns403WhenNotOwnerAndNeverReachesService() {
        // What an EDITOR or a VIEWER of the trip gets: the evaluator knows them, but they do not own the trip
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(mvc.post().uri(LINKS_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        verify(sharingService, never()).createLink(any(), any());
    }

    @Test
    void createWithoutTokenReturns401() {
        assertThat(mvc.post().uri(LINKS_URL)).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(sharingService, tripPermission);
    }

    @Test
    void createForMissingTripReturns404() {
        // The evaluator lets a missing trip through so that the answer is 404, not 403 (CLAUDE.md rule 15)
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);
        when(sharingService.createLink(TRIP_ID, null)).thenThrow(new ResourceNotFoundException("Trip", TRIP_ID));

        assertThat(mvc.post().uri(LINKS_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void createWithNonNumericTripIdReturns400() {
        assertThat(mvc.post().uri("/api/v1/trips/abc/share-links").header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(sharingService);
    }

    private MvcTestResult create(String json) {
        return mvc.post().uri(LINKS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    /** The one place of this class that builds the response, so a new field is added here only. */
    private static ShareLinkResponse link(Instant expiresAt) {
        return new ShareLinkResponse(41L, TOKEN, SharePermission.VIEW, expiresAt, 0, CREATED_AT);
    }

}
