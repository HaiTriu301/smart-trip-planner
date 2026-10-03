package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.common.constant.CacheNames;
import com.trieu.tripplanner.provider.weather.dto.DailyForecast;
import com.trieu.tripplanner.provider.weather.dto.WeatherCondition;
import com.trieu.tripplanner.service.ForecastCache;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * The forecast cache against a real Redis (design.md 8.1 "weather:forecast"), with the real mock weather source
 * behind it. Same approach and same sharing of the application context as PlaceSearchCacheIntegrationTest:
 * "answered from Redis" is proven by swapping the stored answer for a made-up one.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ForecastCacheIntegrationTest {

    private static final BigDecimal LAT = new BigDecimal("16.0678000");
    private static final BigDecimal LNG = new BigDecimal("108.2208000");
    private static final LocalDate OCT_5 = LocalDate.of(2026, 10, 5);
    private static final LocalDate OCT_7 = LocalDate.of(2026, 10, 7);

    /** How Spring names the entry in Redis: the cache name, two colons, then our key. */
    private static final String REDIS_KEY = "weather:forecast::16.0678,108.2208:2026-10-05:2026-10-07";

    private static final DailyForecast MADE_UP = new DailyForecast(OCT_5, WeatherCondition.SNOW, -5.0, -1.0, 100);

    @Autowired
    private ForecastCache forecastCache;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    @AfterEach
    void emptyTheCache() {
        cache().clear();
    }

    @Test
    void firstQuestionStoresTheAnswerInRedisForThreeHours() {
        assertThat(redis.hasKey(REDIS_KEY)).isFalse();

        List<DailyForecast> answer = forecastCache.forecast(LAT, LNG, OCT_5, OCT_7);

        assertThat(answer).extracting(DailyForecast::date).containsExactly(OCT_5, OCT_5.plusDays(1), OCT_7);
        assertThat(redis.hasKey(REDIS_KEY)).isTrue();
        long secondsLeft = redis.getExpire(REDIS_KEY, TimeUnit.SECONDS);
        assertThat(secondsLeft).isBetween(TimeUnit.HOURS.toSeconds(3) - 60, TimeUnit.HOURS.toSeconds(3));
        // Plain JSON with readable dates, and no Java class name in it
        String stored = redis.opsForValue().get(REDIS_KEY);
        assertThat(stored).startsWith("[{").contains("\"date\":\"2026-10-05\"").doesNotContain("com.trieu", "@class");
    }

    @Test
    void sameQuestionIsThenAnsweredFromRedisNotByTheSource() {
        forecastCache.forecast(LAT, LNG, OCT_5, OCT_7);
        // Snow in Đà Nẵng: nothing the source would ever say, so it can only come from Redis
        cache().put(ForecastCache.keyOf(LAT, LNG, OCT_5, OCT_7), List.of(MADE_UP));

        assertThat(forecastCache.forecast(LAT, LNG, OCT_5, OCT_7)).containsExactly(MADE_UP);
        // A point 8 m away is the same point
        assertThat(forecastCache.forecast(new BigDecimal("16.06784"), new BigDecimal("108.22076"), OCT_5, OCT_7))
                .containsExactly(MADE_UP);
    }

    @Test
    void anotherPointOrAnotherRangeOfDaysIsAskedToTheSource() {
        forecastCache.forecast(LAT, LNG, OCT_5, OCT_7);
        cache().put(ForecastCache.keyOf(LAT, LNG, OCT_5, OCT_7), List.of(MADE_UP));

        assertThat(forecastCache.forecast(new BigDecimal("21.0283"), new BigDecimal("105.8542"), OCT_5, OCT_7))
                .doesNotContain(MADE_UP).hasSize(3);
        // One day shorter: another question, even though it overlaps the stored one
        assertThat(forecastCache.forecast(LAT, LNG, OCT_5, OCT_5.plusDays(1))).doesNotContain(MADE_UP).hasSize(2);
    }

    @Test
    void answerReadBackFromRedisIsTheSameListOfTheSameRecords() {
        List<DailyForecast> fromTheSource = forecastCache.forecast(LAT, LNG, OCT_5, OCT_5.plusDays(15));
        assertThat(fromTheSource).hasSize(16);

        List<DailyForecast> fromRedis = forecastCache.forecast(LAT, LNG, OCT_5, OCT_5.plusDays(15));

        // Dates, conditions, temperatures to the decimal: all equal, and real records, not maps
        assertThat(fromRedis).isEqualTo(fromTheSource);
        assertThat(fromRedis.getFirst().condition()).isInstanceOf(WeatherCondition.class);
    }

    @Test
    void bothCachesAreDeclaredAndNothingElse() {
        assertThat(cacheManager.getCacheNames())
                .containsExactlyInAnyOrder(CacheNames.PLACE_SEARCH, CacheNames.WEATHER_FORECAST);
    }

    private Cache cache() {
        return cacheManager.getCache(CacheNames.WEATHER_FORECAST);
    }

}
