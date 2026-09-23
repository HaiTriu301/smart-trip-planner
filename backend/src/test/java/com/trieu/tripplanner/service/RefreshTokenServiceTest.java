package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.config.properties.JwtProperties;
import com.trieu.tripplanner.dto.internal.ClientInfo;
import com.trieu.tripplanner.model.RefreshToken;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.repository.RefreshTokenRepository;
import com.trieu.tripplanner.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final JwtProperties properties = new JwtProperties(
            "unit-test-secret-0123456789abcdef0123456789abcdef0123456789abcdef",
            Duration.ofMinutes(15), Duration.ofDays(7), "smart-trip-planner", false);

    private final User user = TestUsers.verified(5L, "an@example.com");

    @Test
    void issueReturnsRawTokenButStoresOnlyItsSha256() {
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, properties);
        Instant before = Instant.now();

        RefreshTokenService.IssuedRefreshToken issued = service.issue(user, new ClientInfo("Mozilla/5.0", "10.0.0.1"));

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        RefreshToken stored = saved.getValue();

        assertThat(issued.rawToken()).hasSize(64).matches("[A-Za-z0-9_-]+");   // base64url, no padding
        assertThat(stored.getTokenHash()).hasSize(64).matches("[0-9a-f]+")     // sha-256 hex
                .isEqualTo(RefreshTokenService.hash(issued.rawToken()))
                .isNotEqualTo(issued.rawToken());
        assertThat(stored.getUser()).isSameAs(user);
        assertThat(stored.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(stored.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(stored.getRevokedAt()).isNull();
        assertThat(stored.getExpiresAt())
                .isEqualTo(issued.expiresAt())
                .isBetween(before.plus(Duration.ofDays(7)), Instant.now().plus(Duration.ofDays(7)));
    }

    @Test
    void issueGeneratesADifferentTokenEveryTime() {
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, properties);

        String first = service.issue(user, ClientInfo.unknown()).rawToken();
        String second = service.issue(user, ClientInfo.unknown()).rawToken();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void issueTruncatesOversizedUserAgentAndIp() {
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, properties);

        service.issue(user, new ClientInfo("x".repeat(300), "y".repeat(60)));

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getUserAgent()).hasSize(255);
        assertThat(saved.getValue().getIpAddress()).hasSize(45);
    }

    @Test
    void findByRawTokenLooksUpTheHashNotTheRawValue() {
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, properties);
        RefreshToken stored = RefreshToken.builder().user(user).tokenHash(RefreshTokenService.hash("raw-token"))
                .expiresAt(Instant.now().plusSeconds(60)).build();
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash("raw-token"))).thenReturn(java.util.Optional.of(stored));

        assertThat(service.findByRawToken("raw-token")).contains(stored);
    }

    @Test
    void hashIsDeterministicSha256Hex() {
        String hash = RefreshTokenService.hash("abc");

        // Well-known SHA-256("abc")
        assertThat(hash).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(RefreshTokenService.hash("abc")).isEqualTo(hash);
        assertThat(RefreshTokenService.hash("abd")).isNotEqualTo(hash);
    }

    @Test
    void revokeAllDelegatesToRepositoryWithCurrentTime() {
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, properties);
        when(refreshTokenRepository.revokeAllActiveByUserId(eq(5L), any(Instant.class))).thenReturn(3);

        assertThat(service.revokeAll(5L)).isEqualTo(3);
    }

    @Test
    void revokeStampsTheEntity() {
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, properties);
        RefreshToken token = RefreshToken.builder().user(user).tokenHash("h").expiresAt(Instant.now().plusSeconds(60)).build();

        service.revoke(token);

        assertThat(token.isRevoked()).isTrue();
    }

}
