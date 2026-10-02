package com.trieu.tripplanner.provider.weather;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.provider.weather.dto.DailyForecast;
import com.trieu.tripplanner.provider.weather.dto.WeatherCondition;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * No Spring context: the provider needs nothing but its input.
 */
class MockWeatherProviderTest {

    private static final BigDecimal DA_NANG_LAT = new BigDecimal("16.0678000");
    private static final BigDecimal DA_NANG_LNG = new BigDecimal("108.2208000");
    private static final BigDecimal HA_NOI_LAT = new BigDecimal("21.0283000");
    private static final BigDecimal HA_NOI_LNG = new BigDecimal("105.8542000");

    private static final LocalDate FIRST_DAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate A_YEAR_LATER = FIRST_DAY.plusDays(364);

    private final MockWeatherProvider provider = new MockWeatherProvider();

    @Test
    void sameQuestionAlwaysGetsTheSameAnswer() {
        List<DailyForecast> first = provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY, FIRST_DAY.plusDays(6));

        assertThat(provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY, FIRST_DAY.plusDays(6))).isEqualTo(first);
        // Another instance, as after a restart of the application
        assertThat(new MockWeatherProvider().forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY, FIRST_DAY.plusDays(6)))
                .isEqualTo(first);
    }

    @Test
    void forecastOfADayDoesNotDependOnTheRangeItWasAskedIn() {
        DailyForecast alone = provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY.plusDays(3), FIRST_DAY.plusDays(3))
                .getFirst();

        assertThat(provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY, FIRST_DAY.plusDays(6)).get(3))
                .isEqualTo(alone);
    }

    @Test
    void givesOneForecastPerDayOfTheRangeEarliestFirst() {
        assertThat(provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY, FIRST_DAY.plusDays(2)))
                .extracting(DailyForecast::date)
                .containsExactly(FIRST_DAY, FIRST_DAY.plusDays(1), FIRST_DAY.plusDays(2));
        // The last day is included, so a one-day range is one forecast
        assertThat(provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY, FIRST_DAY))
                .extracting(DailyForecast::date)
                .containsExactly(FIRST_DAY);
    }

    @Test
    void anotherPlaceOrAnotherDayGetsAnotherForecast() {
        List<DailyForecast> daNang = provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY, FIRST_DAY.plusDays(29));
        List<DailyForecast> haNoi = provider.forecast(HA_NOI_LAT, HA_NOI_LNG, FIRST_DAY, FIRST_DAY.plusDays(29));

        assertThat(haNoi).extracting(DailyForecast::precipitationProbability)
                .isNotEqualTo(daNang.stream().map(DailyForecast::precipitationProbability).toList());
        // Thirty days in one place are not thirty copies of one day
        assertThat(daNang.stream().map(DailyForecast::precipitationProbability).distinct().count())
                .isGreaterThan(10);
    }

    @Test
    void pointsLessThanElevenMetresApartShareOneForecast() {
        // Both round to 16.0678, 108.2208
        List<DailyForecast> exact = provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY, FIRST_DAY.plusDays(6));
        List<DailyForecast> nextDoor = provider.forecast(new BigDecimal("16.0678400"), new BigDecimal("108.2207600"),
                FIRST_DAY, FIRST_DAY.plusDays(6));
        // The fourth decimal differs: another point
        List<DailyForecast> furtherAway = provider.forecast(new BigDecimal("16.0679000"), DA_NANG_LNG,
                FIRST_DAY, FIRST_DAY.plusDays(6));

        assertThat(nextDoor).isEqualTo(exact);
        assertThat(furtherAway).isNotEqualTo(exact);
    }

    @Test
    void numbersStayInsideWhatAWarmClimateCanShow() {
        List<DailyForecast> year = provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY, A_YEAR_LATER);

        assertThat(year).hasSize(365).allSatisfy(day -> {
            assertThat(day.precipitationProbability()).isBetween(0, 100);
            assertThat(day.tempMax()).isBetween(24.0, 35.0);
            assertThat(day.tempMax() - day.tempMin()).isBetween(3.99, 8.01);
            // One decimal, as the API promises
            assertThat(Math.round(day.tempMin() * 10) / 10.0).isEqualTo(day.tempMin());
            assertThat(Math.round(day.tempMax() * 10) / 10.0).isEqualTo(day.tempMax());
        });
    }

    @Test
    void conditionFollowsTheChanceOfRain() {
        List<DailyForecast> year = provider.forecast(DA_NANG_LAT, DA_NANG_LNG, FIRST_DAY, A_YEAR_LATER);

        assertThat(year).allSatisfy(day -> {
            boolean wet = day.condition() == WeatherCondition.RAIN || day.condition() == WeatherCondition.THUNDERSTORM;
            assertThat(wet)
                    .as("%s with %d%% chance of rain", day.condition(), day.precipitationProbability())
                    .isEqualTo(day.precipitationProbability() >= MockWeatherProvider.RAIN_FROM);
        });
        // Over a year every icon the mock can produce shows up; fog and snow never do
        assertThat(year).extracting(DailyForecast::condition).containsOnly(WeatherCondition.CLEAR,
                WeatherCondition.PARTLY_CLOUDY, WeatherCondition.CLOUDY, WeatherCondition.RAIN,
                WeatherCondition.THUNDERSTORM);
    }

}
