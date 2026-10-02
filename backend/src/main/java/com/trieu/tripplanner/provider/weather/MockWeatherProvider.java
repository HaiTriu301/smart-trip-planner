package com.trieu.tripplanner.provider.weather;

import com.trieu.tripplanner.provider.weather.dto.DailyForecast;
import com.trieu.tripplanner.provider.weather.dto.WeatherCondition;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default weather source: no network, no API key. The numbers are made up, but the same point and the same day
 * always give the same forecast, so tests never turn red by chance and a user who reloads the page sees the same
 * weather (CLAUDE.md rules 20 and 24).
 * <p>
 * Every day asked for gets a forecast, however far away: "only the next sixteen days" is a rule of the service,
 * not of this source. The climate is a warm one (24 to 35 degrees at the warmest hour), so FOG and SNOW never
 * come out of here.
 */
@Component
@ConditionalOnProperty(name = "app.providers.weather", havingValue = "mock", matchIfMissing = true)
public class MockWeatherProvider implements WeatherProvider {

    /** Two points closer than about 11 m share one forecast, like the cache key of design.md 8.1. */
    static final int COORDINATE_SCALE = 4;

    /** From this chance of rain on, the day is a rainy one. */
    static final int RAIN_FROM = 60;

    private static final int PARTLY_CLOUDY_FROM = 20;
    private static final int CLOUDY_FROM = 40;
    private static final int THUNDERSTORM_FROM = 85;

    // Temperatures are drawn in tenths of a degree, so every value has exactly one decimal
    private static final int WARMEST_MAX_TENTHS = 240;
    private static final int WARMEST_SPREAD_TENTHS = 111;   // 24.0 .. 35.0
    private static final int NIGHT_DROP_MIN_TENTHS = 40;
    private static final int NIGHT_DROP_SPREAD_TENTHS = 41;  // 4.0 .. 8.0 below the warmest hour

    @Override
    public List<DailyForecast> forecast(BigDecimal lat, BigDecimal lng, LocalDate from, LocalDate to) {
        long roundedLat = round(lat);
        long roundedLng = round(lng);
        return from.datesUntil(to.plusDays(1))
                .map(date -> forDay(roundedLat, roundedLng, date))
                .toList();
    }

    private static DailyForecast forDay(long roundedLat, long roundedLng, LocalDate date) {
        Random random = new Random(seed(roundedLat, roundedLng, date.toEpochDay()));
        // The chance of rain is drawn first and the condition follows it: never "clear sky, 90% rain"
        int precipitationProbability = random.nextInt(101);
        int maxTenths = WARMEST_MAX_TENTHS + random.nextInt(WARMEST_SPREAD_TENTHS);
        int minTenths = maxTenths - NIGHT_DROP_MIN_TENTHS - random.nextInt(NIGHT_DROP_SPREAD_TENTHS);
        return new DailyForecast(date, conditionFor(precipitationProbability), minTenths / 10.0, maxTenths / 10.0,
                precipitationProbability);
    }

    private static WeatherCondition conditionFor(int precipitationProbability) {
        if (precipitationProbability >= THUNDERSTORM_FROM) {
            return WeatherCondition.THUNDERSTORM;
        }
        if (precipitationProbability >= RAIN_FROM) {
            return WeatherCondition.RAIN;
        }
        if (precipitationProbability >= CLOUDY_FROM) {
            return WeatherCondition.CLOUDY;
        }
        if (precipitationProbability >= PARTLY_CLOUDY_FROM) {
            return WeatherCondition.PARTLY_CLOUDY;
        }
        return WeatherCondition.CLEAR;
    }

    /** The coordinate as a whole number of ten-thousandths of a degree. */
    private static long round(BigDecimal degrees) {
        return degrees.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP).unscaledValue().longValueExact();
    }

    /**
     * One number out of the point and the day. The three inputs are mixed, not just added: neighbouring days and
     * neighbouring points would otherwise give seeds that differ by one, and Random starts almost the same
     * sequence for such seeds.
     */
    private static long seed(long roundedLat, long roundedLng, long epochDay) {
        return mix(mix(mix(roundedLat) ^ roundedLng) ^ epochDay);
    }

    // SplitMix64 finalizer: every input bit changes about half of the output bits
    private static long mix(long value) {
        long z = value + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

}
