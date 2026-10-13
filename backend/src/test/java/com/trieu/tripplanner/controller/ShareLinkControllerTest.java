package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.response.ShareLinkResponse;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

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
    void createReturns201WithTheLinkWhenOwnerAndNeedsNoBody() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);
        when(sharingService.createLink(TRIP_ID)).thenReturn(link());

        // No body and no Content-Type: there is nothing to send
        assertThat(mvc.post().uri(LINKS_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "id": 41, "token": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6", "permission": "VIEW",
                                    "viewCount": 0, "createdAt": "2026-10-13T03:00:00Z" } }
                        """);
    }

    @Test
    void createReturns403WhenNotOwnerAndNeverReachesService() {
        // What an EDITOR or a VIEWER of the trip gets: the evaluator knows them, but they do not own the trip
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(mvc.post().uri(LINKS_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        verify(sharingService, never()).createLink(any());
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
        when(sharingService.createLink(TRIP_ID)).thenThrow(new ResourceNotFoundException("Trip", TRIP_ID));

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

    /** The one place of this class that builds the response, so a new field is added here only. */
    private static ShareLinkResponse link() {
        return new ShareLinkResponse(41L, TOKEN, SharePermission.VIEW, 0, CREATED_AT);
    }

}
