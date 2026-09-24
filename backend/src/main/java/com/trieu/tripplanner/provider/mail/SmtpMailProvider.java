package com.trieu.tripplanner.provider.mail;

import com.trieu.tripplanner.config.properties.AppProperties;
import com.trieu.tripplanner.exception.MailSendException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Real transport over SMTP via Spring's JavaMailSender (configured by spring.mail.*).
 * Local profile points it at MailHog (docker-compose, UI on :8025); prod at Brevo.
 * Only created when app.providers.mail=smtp, which also requires spring.mail.host to be set.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.providers.mail", havingValue = "smtp")
@RequiredArgsConstructor
public class SmtpMailProvider implements MailProvider {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    @Override
    public void send(MailMessage message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, StandardCharsets.UTF_8.name());
            helper.setFrom(appProperties.mailFrom());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.htmlBody(), true);
            mailSender.send(mime);
            log.info("Sent mail to={} subject=\"{}\"", message.to(), message.subject());
        }
        catch (MessagingException | MailException ex) {
            throw new MailSendException(message.to(), ex);
        }
    }

}
