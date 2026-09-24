package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.common.util.SecureTokens;
import com.trieu.tripplanner.exception.InvalidTokenException;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.VerificationToken;
import com.trieu.tripplanner.model.enums.VerificationTokenType;
import com.trieu.tripplanner.repository.VerificationTokenRepository;
import com.trieu.tripplanner.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationTokenServiceTest {

    @Mock
    private VerificationTokenRepository repository;

    @InjectMocks
    private VerificationTokenService service;

    private final User user = TestUsers.unverified(5L, "an@example.com");

    @Test
    void issueStoresHashWithTypeTtlAndInvalidatesOlderTokensOfSameType() {
        when(repository.invalidateActive(eq(5L), eq(VerificationTokenType.EMAIL_VERIFY), any(Instant.class))).thenReturn(1);
        Instant before = Instant.now();

        String raw = service.issue(user, VerificationTokenType.EMAIL_VERIFY);

        ArgumentCaptor<VerificationToken> saved = ArgumentCaptor.forClass(VerificationToken.class);
        verify(repository).save(saved.capture());
        VerificationToken stored = saved.getValue();

        assertThat(raw).hasSize(64).matches("[A-Za-z0-9_-]+");
        assertThat(stored.getTokenHash()).isEqualTo(SecureTokens.sha256Hex(raw)).isNotEqualTo(raw);
        assertThat(stored.getType()).isEqualTo(VerificationTokenType.EMAIL_VERIFY);
        assertThat(stored.getUser()).isSameAs(user);
        assertThat(stored.getUsedAt()).isNull();
        assertThat(stored.getExpiresAt())
                .isBetween(before.plus(Duration.ofHours(24)), Instant.now().plus(Duration.ofHours(24)));
    }

    @Test
    void passwordResetTokensLiveOneHour() {
        Instant before = Instant.now();

        service.issue(user, VerificationTokenType.PASSWORD_RESET);

        ArgumentCaptor<VerificationToken> saved = ArgumentCaptor.forClass(VerificationToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getExpiresAt())
                .isBetween(before.plus(Duration.ofHours(1)), Instant.now().plus(Duration.ofHours(1)));
    }

    @Test
    void consumeMarksAValidTokenUsedAndReturnsIt() {
        VerificationToken token = live(VerificationTokenType.EMAIL_VERIFY);
        when(repository.findByTokenHash(SecureTokens.sha256Hex("raw"))).thenReturn(Optional.of(token));

        VerificationToken consumed = service.consume("raw", VerificationTokenType.EMAIL_VERIFY);

        assertThat(consumed).isSameAs(token);
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void consumeRejectsBlankAndUnknownTokens() {
        when(repository.findByTokenHash(SecureTokens.sha256Hex("ghost"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consume(null, VerificationTokenType.EMAIL_VERIFY))
                .isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> service.consume("ghost", VerificationTokenType.EMAIL_VERIFY))
                .isInstanceOf(InvalidTokenException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void consumeRejectsTokenOfAnotherType() {
        // A password-reset link must not verify an email, and vice versa
        VerificationToken reset = live(VerificationTokenType.PASSWORD_RESET);
        when(repository.findByTokenHash(SecureTokens.sha256Hex("raw"))).thenReturn(Optional.of(reset));

        assertThatThrownBy(() -> service.consume("raw", VerificationTokenType.EMAIL_VERIFY))
                .isInstanceOf(InvalidTokenException.class);
        assertThat(reset.isUsed()).isFalse();
    }

    @Test
    void consumeRejectsAlreadyUsedToken() {
        VerificationToken used = live(VerificationTokenType.EMAIL_VERIFY);
        used.markUsed(Instant.now().minusSeconds(60));
        when(repository.findByTokenHash(SecureTokens.sha256Hex("raw"))).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.consume("raw", VerificationTokenType.EMAIL_VERIFY))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void consumeRejectsExpiredTokenWithoutMarkingIt() {
        VerificationToken expired = VerificationToken.builder().user(user).tokenHash("h")
                .type(VerificationTokenType.EMAIL_VERIFY).expiresAt(Instant.now().minusSeconds(1)).build();
        when(repository.findByTokenHash(SecureTokens.sha256Hex("raw"))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.consume("raw", VerificationTokenType.EMAIL_VERIFY))
                .isInstanceOf(InvalidTokenException.class);
        assertThat(expired.isUsed()).isFalse();
    }

    private VerificationToken live(VerificationTokenType type) {
        return VerificationToken.builder().user(user).tokenHash("h").type(type)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(30))).build();
    }

}
