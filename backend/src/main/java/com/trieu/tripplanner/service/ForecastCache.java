package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.constant.CacheNames;
import com.trieu.tripplanner.config.properties.AppProperties;
import com.trieu.tripplanner.provider.weather.WeatherProvider;
import com.trieu.tripplanner.provider.weather.dto.DailyForecast;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Temporary copies of weather forecasts (design.md 8.1 "weather:forecast"). Stands between WeatherService and
 * the weather source: the first time a point and a range of days are asked for, the source answers and the
 * answer is stored in Redis; for the next 3 hours the same question, from any user, is answered from Redis.
 * <p>
 * A bean of its own for the same reasons as {@link PlaceSearchCache}: the cache works through a proxy and only
 * sees calls from another bean, and it does not care whether the source is the mock or a real service.
 */
@Component
@RequiredArgsConstructor
public class ForecastCache {

    /** Two points closer than about 11 m get the same weather. */
    static final int COORDINATE_SCALE = 4;

    private final WeatherProvider weatherProvider;
    private final AppProperties appProperties;

    /**
     * What the weather source answers for this point and these days, from Redis when the same question was
     * asked in the last 3 hours. The source may skip days; whatever it answered is stored as is.
     */
    @Cacheable(cacheNames = CacheNames.WEATHER_FORECAST, key = "#root.target.keyOf(#lat, #lng, #from, #to)")
    public List<DailyForecast> forecast(BigDecimal lat, BigDecimal lng, LocalDate from, LocalDate to) {
        return weatherProvider.forecast(lat, lng, from, to);
    }

    /** The key of this question for the weather source in use, named as {@code app.providers.weather} names it. */
    public String keyOf(BigDecimal lat, BigDecimal lng, LocalDate from, LocalDate to) {
        return keyOf(appProperties.providers().weather(), lat, lng, from, to);
    }

    /**
     * Which questions share one stored answer: the source, the point rounded to four decimals, and the exact
     * range of days. The range is part of the key because the source is asked for exactly that range
     * (design.md 8.1). The source is part of it so that, after a switch of {@code app.providers.weather}, the
     * made-up numbers of the mock are not shown as a real forecast for three hours, nor the other way round
     * (BUG-PLACE-004).
     *
     * @return for example {@code open-meteo|16.0678,108.2208:2026-10-05:2026-10-07}
     */
    public static String keyOf(String source, BigDecimal lat, BigDecimal lng, LocalDate from, LocalDate to) {
        return source + "|" + rounded(lat) + "," + rounded(lng) + ":" + from + ":" + to;
    }

    private static String rounded(BigDecimal degrees) {
        return degrees.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

}
