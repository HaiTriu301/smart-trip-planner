package com.trieu.tripplanner.provider.mail;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default provider: no network, no SMTP. Logs recipient + subject (never the body, it carries the token)
 * and keeps every message so tests can read the link back. Thread-safe because MailService runs @Async.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.providers.mail", havingValue = "mock", matchIfMissing = true)
public class MockMailProvider implements MailProvider {

    private final List<MailMessage> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(MailMessage message) {
        log.info("[mock-mail] to={} subject=\"{}\" ({} chars)", message.to(), message.subject(),
                message.htmlBody().length());
        sent.add(message);
    }

    /** Messages sent so far, oldest first. */
    public List<MailMessage> sent() {
        return Collections.unmodifiableList(sent);
    }

    public void clear() {
        sent.clear();
    }

}
