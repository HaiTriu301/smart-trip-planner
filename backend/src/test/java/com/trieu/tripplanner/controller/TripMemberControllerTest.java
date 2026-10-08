package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.request.AcceptInvitationRequest;
import com.trieu.tripplanner.dto.request.InviteMemberRequest;
import com.trieu.tripplanner.dto.response.MemberResponse;
import com.trieu.tripplanner.dto.response.TripRole;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.ForbiddenException;
import com.trieu.tripplanner.exception.InvalidTokenException;
import com.trieu.tripplanner.exception.MemberAlreadyExistsException;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.model.enums.MemberRole;
import com.trieu.tripplanner.model.enums.MemberStatus;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.security.permission.TripPermissionEvaluator;
import com.trieu.tripplanner.service.SharingService;
import com.trieu.tripplanner.support.TestUsers;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
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

    // ---- GET /trips/{tripId}/members -----------------------------------------------------------------------------

    @Test
    void listReturnsOwnerFirstWhenViewAllowedAndLeaksNoPassword() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(sharingService.listMembers(TRIP_ID)).thenReturn(List.of(
                new MemberResponse(null, 7L, "Chủ chuyến", "owner@example.com", null, TripRole.OWNER,
                        MemberStatus.ACCEPTED, null, null),
                new MemberResponse(31L, 9L, "Bạn đồng hành", "friend@example.com", "https://img/9.png",
                        TripRole.EDITOR, MemberStatus.ACCEPTED, INVITED_AT, INVITED_AT.plusSeconds(60)),
                new MemberResponse(32L, null, null, "waiting@example.com", null, TripRole.VIEWER,
                        MemberStatus.PENDING, INVITED_AT.plusSeconds(120), null)));

        MvcTestResult result = mvc.get().uri(MEMBERS_URL).header(HttpHeaders.AUTHORIZATION, bearer).exchange();

        assertThat(result).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "success": true,
                  "data": [ { "memberId": null, "userId": 7, "fullName": "Chủ chuyến", "role": "OWNER", "status": "ACCEPTED" },
                            { "memberId": 31, "userId": 9, "fullName": "Bạn đồng hành", "avatarUrl": "https://img/9.png",
                              "role": "EDITOR", "status": "ACCEPTED", "acceptedAt": "2026-10-08T03:01:00Z" },
                            { "memberId": 32, "userId": null, "fullName": null, "email": "waiting@example.com",
                              "role": "VIEWER", "status": "PENDING", "acceptedAt": null } ] }
                """);
        assertThat(new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8))
                .doesNotContainIgnoringCase("password");
    }

    @Test
    void listReturns403WhenViewDeniedAndNeverReachesService() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(false);

        assertThat(mvc.get().uri(MEMBERS_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        verify(sharingService, never()).listMembers(any());
    }

    @Test
    void listReturns404WhenTripDoesNotExist() {
        when(tripPermission.canView(eq(TRIP_ID), any())).thenReturn(true);
        when(sharingService.listMembers(TRIP_ID)).thenThrow(new ResourceNotFoundException("Trip", TRIP_ID));

        assertThat(mvc.get().uri(MEMBERS_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void listWithoutTokenReturns401() {
        assertThat(mvc.get().uri(MEMBERS_URL)).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(sharingService, tripPermission);
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

    // ---- POST /trips/{tripId}/members/accept ---------------------------------------------------------------------

    @Test
    void acceptReturns200WithTheMembershipAndTakesTheUserFromTheToken() {
        when(sharingService.accept(TRIP_ID, 7L, new AcceptInvitationRequest("raw-token")))
                .thenReturn(new MemberResponse(31L, 7L, "Test owner@example.com", "owner@example.com", null,
                        TripRole.VIEWER, MemberStatus.ACCEPTED, INVITED_AT, INVITED_AT.plusSeconds(60)));

        assertThat(accept("""
                { "token": "raw-token" }
                """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": { "memberId": 31, "userId": 7, "role": "VIEWER", "status": "ACCEPTED",
                                    "acceptedAt": "2026-10-08T03:01:00Z" } }
                        """);
        // The caller has no permission on the trip yet, so the evaluator must not be asked
        verifyNoInteractions(tripPermission);
    }

    @Test
    void acceptWithoutTokenReturns401() {
        assertThat(mvc.post().uri(MEMBERS_URL + "/accept").contentType(MediaType.APPLICATION_JSON).content("""
                { "token": "raw-token" }
                """))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(sharingService);
    }

    @Test
    void acceptWithBlankInvitationTokenReturns400OnTheTokenField() {
        assertThat(accept("""
                { "token": "  " }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "token", "message": "Thiếu mã xác thực" } ] }
                        """);
        verifyNoInteractions(sharingService);
    }

    @Test
    void acceptWithARejectedTokenReturns400InvalidToken() {
        when(sharingService.accept(eq(TRIP_ID), eq(7L), any())).thenThrow(new InvalidTokenException("expired"));

        assertThat(accept("""
                { "token": "old-token" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "INVALID_TOKEN",
                          "message": "Liên kết không hợp lệ hoặc đã hết hạn, vui lòng yêu cầu lại" }
                        """);
    }

    @Test
    void acceptForAnotherEmailReturns403WithTheGenericMessage() {
        when(sharingService.accept(eq(TRIP_ID), eq(7L), any()))
                .thenThrow(new ForbiddenException("invitation 31 was sent to another email"));

        assertThat(accept("""
                { "token": "forwarded-token" }
                """))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "FORBIDDEN",
                          "message": "Bạn không có quyền thực hiện thao tác này" }
                        """);
    }

    private MvcTestResult accept(String body) {
        return mvc.post().uri(MEMBERS_URL + "/accept").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult invite(String body) {
        return mvc.post().uri(MEMBERS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

}
