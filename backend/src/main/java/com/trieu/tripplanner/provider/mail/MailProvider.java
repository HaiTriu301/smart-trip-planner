package com.trieu.tripplanner.provider.mail;

/**
 * Port for sending email (design.md 7.1). Implementations are chosen by {@code app.providers.mail}:
 * {@code smtp} talks to a real server (MailHog locally, Brevo in prod), {@code mock} records messages in memory.
 * Services depend on this interface only, never on JavaMailSender (CLAUDE.md rule 19).
 */
public interface MailProvider {

    /**
     * @throws com.trieu.tripplanner.exception.MailSendException when the transport rejects the message
     */
    void send(MailMessage message);

}
