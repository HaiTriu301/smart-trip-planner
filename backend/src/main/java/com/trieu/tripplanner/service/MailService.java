package com.trieu.tripplanner.service;

import com.trieu.tripplanner.config.properties.AppProperties;
import com.trieu.tripplanner.provider.mail.MailMessage;
import com.trieu.tripplanner.provider.mail.MailProvider;
import java.util.Locale;
import java.util.Map;
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
    static final String TRIP_INVITATION_TEMPLATE = "mail/trip-invitation";
    static final String VERIFY_EMAIL_PATH = "/verify-email?token=";
    static final String RESET_PASSWORD_PATH = "/reset-password?token=";
    /** Query string read by the /invite page (design.md 10.2 "Quy ước Sharing API"): trip id first, then the token. */
    static final String TRIP_INVITATION_PATH = "/invite?trip=%d&token=%s";

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

    /**
     * Invitation to join a trip (design.md rule 14.23). Sent to the invited address whether or not it belongs to
     * an account yet, so the greeting has no name; the subject names the inviter and the trip instead.
     *
     * @param canEdit  true for an EDITOR invitation, false for VIEWER; the mail says which one
     * @param rawToken the one-time token, URL-safe already (SecureTokens.generate), carried by the link only
     */
    @Async
    public void sendInvitationMail(String toEmail, String inviterName, String tripTitle, boolean canEdit,
                                   Long tripId, String rawToken) {
        Map<String, Object> variables = Map.of(
                "inviterName", inviterName,
                "tripTitle", tripTitle,
                "canEdit", canEdit,
                "actionUrl", appProperties.frontendUrl() + TRIP_INVITATION_PATH.formatted(tripId, rawToken));
        send(toEmail, "mail.trip-invitation.subject", new Object[] {inviterName, tripTitle},
                TRIP_INVITATION_TEMPLATE, variables);
    }

    private void send(String toEmail, String subjectKey, String template, String fullName, String actionUrl) {
        send(toEmail, subjectKey, null, template, Map.of("fullName", fullName, "actionUrl", actionUrl));
    }

    private void send(String toEmail, String subjectKey, Object[] subjectArgs, String template,
                      Map<String, Object> variables) {
        Context context = new Context(Locale.forLanguageTag("vi"));
        context.setVariables(variables);
        String html = templateEngine.process(template, context);
        String subject = messageSource.getMessage(subjectKey, subjectArgs, Locale.ROOT);

        // Log the recipient only: the body contains the one-time token
        log.debug("Sending \"{}\" to {}", subject, toEmail);
        mailProvider.send(new MailMessage(toEmail, subject, html));
    }

}
