package com.trieu.tripplanner.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The one clock of the application. Code that needs "now" or "today" asks this bean instead of calling
 * {@code LocalDate.now()} itself, so a test can replace it with a clock that stands still at a chosen moment.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    /** UTC: a date in the time zone of an account is derived from it where it is needed (design.md rule 14.22). */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}
