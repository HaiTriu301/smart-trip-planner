package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Which forecast questions share one stored answer. Only the key is checked here, no Spring and no Redis; that
 * the key is really what Redis stores under is ForecastCacheIntegrationTest.
 */
class ForecastCacheTest {

    private static final BigDecimal LAT = new BigDecimal("16.0678000");
    private static final BigDecimal LNG = new BigDecimal("108.2208000");
    private static final LocalDate OCT_5 = LocalDate.of(2026, 10, 5);
    private static final LocalDate OCT_7 = LocalDate.of(2026, 10, 7);

    @Test
    void keyShowsTheSourceThePointAndTheRangeOfDaysInReadableForm() {
        assertThat(ForecastCache.keyOf("open-meteo", LAT, LNG, OCT_5, OCT_7))
                .isEqualTo("open-meteo|16.0678,108.2208:2026-10-05:2026-10-07");
    }

    @Test
    void sameQuestionAskedToAnotherSourceIsAnotherQuestion() {
        // The made-up numbers of the mock must not be shown as a real forecast after a switch (BUG-PLACE-004)
        assertThat(ForecastCache.keyOf("mock", LAT, LNG, OCT_5, OCT_7))
                .isEqualTo("mock|16.0678,108.2208:2026-10-05:2026-10-07")
                .isNotEqualTo(ForecastCache.keyOf("open-meteo", LAT, LNG, OCT_5, OCT_7));
    }

    @Test
    void pointsAFewMetresApartShareAnAnswerAndAnotherRangeDoesNot() {
        String key = ForecastCache.keyOf("mock", LAT, LNG, OCT_5, OCT_7);

        // 8 m away: the same weather
        assertThat(ForecastCache.keyOf("mock", new BigDecimal("16.06784"), new BigDecimal("108.22076"), OCT_5, OCT_7))
                .isEqualTo(key);
        assertThat(ForecastCache.keyOf("mock", LAT, LNG, OCT_5, OCT_5.plusDays(1))).isNotEqualTo(key);
    }

}
