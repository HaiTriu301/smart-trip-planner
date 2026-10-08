package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.request.InviteMemberRequest;
import com.trieu.tripplanner.dto.response.MemberResponse;
import com.trieu.tripplanner.dto.response.TripRole;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.MemberAlreadyExistsException;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.model.enums.MemberStatus;
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
@WebMvcTest(TripMemberController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class TripMemberControllerTest {

    private static final long TRIP_ID = 5L;
    private static final String MEMBERS_URL = "/api/v1/trips/5/members";
    private static final Instant INVITED_AT = Instant.parse("2026-10-08T03:00:00Z");

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

    // ---- POST /trips/{tripId}/members ----------------------------------------------------------------------------

    @Test
    void inviteReturns201WithThePendingMemberWhenOwner() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);
        when(sharingService.invite(TRIP_ID, new InviteMemberRequest("friend@example.com", MemberRole.EDITOR)))
                .thenReturn(new MemberResponse(31L, null, null, "friend@example.com", null, TripRole.EDITOR,
                        MemberStatus.PENDING, INVITED_AT, null));

        assertThat(invite("""
                { "email": "friend@example.com", "role": "EDITOR" }
                """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "memberId": 31, "userId": null, "email": "friend@example.com", "role": "EDITOR",
                                    "status": "PENDING", "invitedAt": "2026-10-08T03:00:00Z", "acceptedAt": null } }
                        """);
    }

    @Test
    void inviteReturns403WhenNotOwnerAndNeverReachesService() {
        // An EDITOR passes canEdit but not isOwner: managing members is owner-only (design.md 6.2)
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(invite("""
                { "email": "friend@example.com", "role": "VIEWER" }
                """))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        verify(sharingService, never()).invite(any(), any());
    }

    @Test
    void inviteWithoutTokenReturns401() {
        assertThat(mvc.post().uri(MEMBERS_URL).contentType(MediaType.APPLICATION_JSON).content("""
                { "email": "friend@example.com", "role": "VIEWER" }
                """))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(sharingService, tripPermission);
    }

    @Test
    void inviteWithMalformedEmailReturns400OnTheEmailField() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(invite("""
                { "email": "not-an-email", "role": "VIEWER" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "email", "message": "Email không đúng định dạng" } ] }
                        """);
        verifyNoInteractions(sharingService);
    }

    @Test
    void inviteWithoutRoleReturns400OnTheRoleField() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);

        assertThat(invite("""
                { "email": "friend@example.com" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "role", "message": "Thiếu vai trò của thành viên (EDITOR hoặc VIEWER)" } ] }
                        """);
        verifyNoInteractions(sharingService);
    }

    @Test
    void inviteAsOwnerRoleReturns400BecauseTheValueDoesNotExist() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);

        // OWNER is not a MemberRole: the body cannot even be parsed, so a second owner can never be created
        assertThat(invite("""
                { "email": "friend@example.com", "role": "OWNER" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(sharingService);
    }

    @Test
    void inviteOfTheOwnersEmailReturns400OnTheEmailField() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);
        when(sharingService.invite(eq(TRIP_ID), any())).thenThrow(BusinessRuleException.invalidField(
                "email", "error.member.owner-email", "Trip 5: the owner invited their own email"));

        assertThat(invite("""
                { "email": "owner@example.com", "role": "VIEWER" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "email",
                                         "message": "Đây là email của chính bạn, chủ chuyến đi không cần được mời" } ] }
                        """);
    }

    @Test
    void inviteOfAnAcceptedMemberReturns409() {
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);
        when(sharingService.invite(eq(TRIP_ID), any())).thenThrow(new MemberAlreadyExistsException(TRIP_ID, 31L));

        assertThat(invite("""
                { "email": "friend@example.com", "role": "VIEWER" }
                """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "MEMBER_ALREADY_EXISTS",
                          "message": "Người này đã là thành viên của chuyến đi" }
                        """);
    }

    @Test
    void inviteForMissingTripReturns404() {
        // The evaluator lets a missing trip through on purpose; the service answers 404 (CLAUDE.md rule 15)
        when(tripPermission.isOwner(eq(TRIP_ID), any())).thenReturn(true);
        when(sharingService.invite(eq(TRIP_ID), any())).thenThrow(new ResourceNotFoundException("Trip", TRIP_ID));

        assertThat(invite("""
                { "email": "friend@example.com", "role": "VIEWER" }
                """))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    private MvcTestResult invite(String body) {
        return mvc.post().uri(MEMBERS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

}
