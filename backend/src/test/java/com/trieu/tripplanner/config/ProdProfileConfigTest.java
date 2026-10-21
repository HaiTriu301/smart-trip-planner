package com.trieu.tripplanner.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * The settings of the prod profile that keep the deployment safe (design.md 17.3, 17.4; WORKFLOW Task 4.8,
 * decision 16). Nothing runs in prod during the tests, so a slip in application-prod.yml (the cookie losing its
 * Secure flag, mail sent in clear text, a real payment provider switched on early) would otherwise only show on the
 * live server. Reads the two YAML files the way Spring layers them, without starting the application.
 */
class ProdProfileConfigTest {

    /** application-prod.yml first: Spring lets a profile file override application.yml */
    private static final List<PropertySource<?>> SOURCES = new ArrayList<>();

    @BeforeAll
    static void loadProdOverBase() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        SOURCES.addAll(loader.load("prod", new ClassPathResource("application-prod.yml")));
        SOURCES.addAll(loader.load("base", new ClassPathResource("application.yml")));
    }

    /** The value the prod profile ends up with, or null when neither file sets it. */
    private static String value(String key) {
        return SOURCES.stream()
                .map(source -> source.getProperty(key))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .findFirst()
                .orElse(null);
    }

    @Test
    void refreshCookieOnlyTravelsOverHttps() {
        assertThat(value("app.jwt.cookie-secure")).isEqualTo("true");
    }

    @Test
    void callerAddressAndHttpsComeFromTheProxyHeaders() {
        // nginx overwrites X-Forwarded-For with the real address; Tomcat fills getRemoteAddr() from it (design.md 6.3)
        assertThat(value("server.forward-headers-strategy")).isEqualTo("native");
    }

    @Test
    void mailIsSentOverAnEncryptedAuthenticatedConnection() {
        assertThat(value("spring.mail.properties.mail.smtp.auth")).isEqualTo("true");
        assertThat(value("spring.mail.properties.mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(value("spring.mail.properties.mail.smtp.starttls.required")).isEqualTo("true");
    }

    @Test
    void slowMailServiceDoesNotTurnTheHealthCheckDown() {
        assertThat(value("management.health.mail.enabled")).isEqualTo("false");
        // Same reasoning as Redis, which only holds temporary copies (design.md 8.1)
        assertThat(value("management.health.redis.enabled")).isEqualTo("false");
    }

    @Test
    void visitorsGetRealMapAndWeatherButPaymentAndAiStayMocked() {
        assertThat(value("app.providers.map")).isEqualTo("osm");
        assertThat(value("app.providers.weather")).isEqualTo("open-meteo");
        assertThat(value("app.providers.mail")).isEqualTo("smtp");
        // Not built yet: switching them on belongs to Phase 6 (Stripe) and Phase 7 (AI)
        assertThat(value("app.providers.payment")).isEqualTo("mock");
        assertThat(value("app.providers.ai")).isEqualTo("mock");
    }

    @Test
    void rateLimitsStayOn() {
        // The public deployment was allowed only once the limits existed (design.md 8.2, Task 4.7)
        assertThat(value("app.rate-limit.enabled")).isEqualTo("true");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "spring.datasource.url", "spring.datasource.username", "spring.datasource.password",
            "spring.mail.host", "spring.mail.username", "spring.mail.password",
            "app.jwt.secret", "app.frontend-url", "app.mail-from"})
    void secretsAndAddressesComeFromEnvironmentVariables(String key) {
        // A placeholder such as ${DB_PASSWORD}, never a value written in the file (CLAUDE.md rule 18)
        assertThat(value(key)).matches("\\$\\{[A-Z0-9_]+}");
    }
}
