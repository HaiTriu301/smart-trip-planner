package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.response.UserResponse;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.model.enums.Plan;
import com.trieu.tripplanner.model.enums.Role;
import com.trieu.tripplanner.model.enums.UserStatus;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.service.UserService;
import com.trieu.tripplanner.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class UserControllerTest {

    private static final String ME_URL = "/api/v1/users/me";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserService userService;

    @Test
    void meReturnsProfileOfTheUserInsideTheToken() {
        String token = jwtTokenProvider.generateAccessToken(TestUsers.verified(7L, "an@example.com")).token();
        when(userService.getProfile(7L)).thenReturn(new UserResponse(7L, "an@example.com", "Nguyễn An", null,
                "Asia/Ho_Chi_Minh", "vi", Role.USER, Plan.FREE, null, true, UserStatus.ACTIVE,
                Instant.parse("2026-09-20T10:00:00Z")));

        assertThat(mvc.get().uri(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true, "data": { "id": 7, "email": "an@example.com", "fullName": "Nguyễn An", "plan": "FREE" } }
                        """);
        verify(userService).getProfile(7L);
    }

    @Test
    void meWithoutTokenReturns401AndNeverReachesService() {
        assertThat(mvc.get().uri(ME_URL))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(userService);
    }

    @Test
    void meWithExpiredTokenReturnsTokenExpired() {
        String expired = jwtTokenProvider.generateAccessToken(TestUsers.verified(7L, "an@example.com"),
                Instant.now().minus(Duration.ofHours(2))).token();

        assertThat(mvc.get().uri(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("TOKEN_EXPIRED");
        verifyNoInteractions(userService);
    }

    @Test
    void meForDeletedUserReturns404() {
        String token = jwtTokenProvider.generateAccessToken(TestUsers.verified(99L, "gone@example.com")).token();
        when(userService.getProfile(99L)).thenThrow(new ResourceNotFoundException("User", 99L));

        assertThat(mvc.get().uri(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

}
