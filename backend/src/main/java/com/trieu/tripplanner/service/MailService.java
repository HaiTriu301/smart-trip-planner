package com.trieu.tripplanner.service;

import com.trieu.tripplanner.config.properties.AppProperties;
import com.trieu.tripplanner.provider.mail.MailMessage;
import com.trieu.tripplanner.provider.mail.MailProvider;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Composes the application's emails (templates/mail/*.html) and hands them to the MailProvider.
 * Every public method is @Async: the HTTP request that triggered the mail returns immediately and a failed
 * delivery only shows up in the log (design.md 14.17). Parameters are plain values, not entities, because the
 * call runs on another thread outside the caller's transaction.
 * <p>
 * Callers must invoke these methods through the Spring bean; a call from inside this class would bypass the proxy
 * and run synchronously.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    static final String VERIFY_EMAIL_TEMPLATE = "mail/verify-email";
    static final String RESET_PASSWORD_TEMPLATE = "mail/reset-password";
    static final String VERIFY_EMAIL_PATH = "/verify-email?token=";
    static final String RESET_PASSWORD_PATH = "/reset-password?token=";

    private final TemplateEngine templateEngine;
    private final MailProvider mailProvider;
    private final MessageSource messageSource;
    private final AppProperties appProperties;

    @Async
    public void sendVerificationMail(String toEmail, String fullName, String rawToken) {
        send(toEmail, "mail.verify-email.subject", VERIFY_EMAIL_TEMPLATE, fullName,
                appProperties.frontendUrl() + VERIFY_EMAIL_PATH + rawToken);
    }

    @Async
    public void sendPasswordResetMail(String toEmail, String fullName, String rawToken) {
        send(toEmail, "mail.reset-password.subject", RESET_PASSWORD_TEMPLATE, fullName,
                appProperties.frontendUrl() + RESET_PASSWORD_PATH + rawToken);
    }

    private void send(String toEmail, String subjectKey, String template, String fullName, String actionUrl) {
        Context context = new Context(Locale.forLanguageTag("vi"));
        context.setVariable("fullName", fullName);
        context.setVariable("actionUrl", actionUrl);
        String html = templateEngine.process(template, context);
        String subject = messageSource.getMessage(subjectKey, null, Locale.ROOT);

        // Log the recipient only: the body contains the one-time token
        log.debug("Sending \"{}\" to {}", subject, toEmail);
        mailProvider.send(new MailMessage(toEmail, subject, html));
    }

}
