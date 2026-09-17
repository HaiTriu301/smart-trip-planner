package com.trieu.tripplanner.common.constant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ErrorCodeTest {

    private final ResourceBundleMessageSource messageSource = createMessageSource();

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyErrorCodeHasMessageInMessagesProperties(ErrorCode errorCode) {
        assertThatNoException()
                .as("messages.properties is missing key '%s'", errorCode.getMessageKey())
                .isThrownBy(() -> messageSource.getMessage(errorCode.getMessageKey(), null, Locale.ROOT));
    }

    @Test
    void messagesPropertiesIsReadAsUtf8() {
        String message = messageSource.getMessage(ErrorCode.VALIDATION_ERROR.getMessageKey(), null, Locale.ROOT);

        assertThat(message).isEqualTo("Dữ liệu không hợp lệ");
    }

    private static ResourceBundleMessageSource createMessageSource() {
        // Same settings as spring.messages.* in application.yml
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
