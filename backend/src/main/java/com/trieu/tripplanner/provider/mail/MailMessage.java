package com.trieu.tripplanner.provider.mail;

/**
 * One outgoing email, already rendered. The provider only transports it.
 *
 * @param to       recipient address
 * @param subject  subject line (Vietnamese, from messages.properties)
 * @param htmlBody rendered Thymeleaf template
 */
public record MailMessage(String to, String subject, String htmlBody) {
}
