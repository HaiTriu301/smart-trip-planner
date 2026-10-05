package com.trieu.tripplanner.config;

import com.trieu.tripplanner.common.constant.CacheNames;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import com.trieu.tripplanner.provider.map.dto.RouteLeg;
import com.trieu.tripplanner.provider.weather.dto.DailyForecast;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import tools.jackson.databind.ObjectMapper;

/**
 * Caches in Redis (design.md 8.1). Redis keeps temporary copies of what outside sources answered, shared by
 * every user; nothing in it is the only copy of anything.
 * <p>
 * Each cache is declared here with two facts: how long an answer may be reused, and the exact Java type it
 * stores. The type is fixed per cache on purpose. The alternative, a serializer that writes the class name next
 * to the data, hands records back as plain maps, and lets whatever sits in Redis decide which class gets built.
 * <p>
 * A Redis that is down or slow is not an error of the request: the cache step is skipped, the source is asked
 * as if there were no cache, and the failure is logged (see {@link #errorHandler()}). How long a request may
 * wait for Redis before that happens is {@code spring.data.redis.timeout} in application.yml.
 * <p>
 * Entries are written to Redis before the cached method returns. Spring Data Redis would otherwise hand the
 * write to a background task when the driver allows it (Lettuce does): slightly faster, but a read right
 * after the write may not see it, and a failed write never reaches {@link #errorHandler()} (BUG-PLACE-003).
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /** Places rarely move or change their name. */
    static final Duration PLACE_SEARCH_TTL = Duration.ofHours(24);

    /** Roads change slowly, and the estimate of the time does not follow the traffic of the moment anyway. */
    static final Duration ROUTE_LEGS_TTL = Duration.ofHours(24);

    /** Forecasts are revised several times a day. */
    static final Duration WEATHER_FORECAST_TTL = Duration.ofHours(3);

    @Bean
    RedisCacheManagerBuilderCustomizer declaredCaches(ObjectMapper objectMapper, RedisConnectionFactory connectionFactory) {
        return builder -> builder
                .cacheWriter(RedisCacheWriter.create(connectionFactory, writer -> writer.immediateWrites()))
                .withCacheConfiguration(CacheNames.PLACE_SEARCH,
                        listCache(PLACE_SEARCH_TTL, PlaceResult.class, objectMapper))
                .withCacheConfiguration(CacheNames.ROUTE_LEGS,
                        listCache(ROUTE_LEGS_TTL, RouteLeg.class, objectMapper))
                .withCacheConfiguration(CacheNames.WEATHER_FORECAST,
                        listCache(WEATHER_FORECAST_TTL, DailyForecast.class, objectMapper))
                // A typo in a cache name must fail, not create a cache that never expires
                .disableCreateOnMissingCache();
    }

    /**
     * What happens when Redis cannot be read or written: one WARN line naming the cache, the key and the reason,
     * then the call goes on without the cache. Never the stack trace: with Redis down, every request would
     * print one (CLAUDE.md rule 12 asks for context, not for noise).
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler(LogFactory.getLog(CacheConfig.class), false) {
            @Override
            protected void logCacheError(Supplier<String> message, RuntimeException ex) {
                // Spring's own line names the cache and the key; the reason is added, the stack trace is not
                getLogger().warn(message.get() + ": " + ex.getMessage());
            }
        };
    }

    /** A cache whose every entry is a list of {@code elementType}, stored as JSON for {@code timeToLive}. */
    private static <T> RedisCacheConfiguration listCache(Duration timeToLive, Class<T> elementType,
                                                         ObjectMapper objectMapper) {
        JacksonJsonRedisSerializer<List<T>> json = new JacksonJsonRedisSerializer<>(objectMapper,
                objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(timeToLive)
                .serializeValuesWith(SerializationPair.fromSerializer(json));
    }

}
