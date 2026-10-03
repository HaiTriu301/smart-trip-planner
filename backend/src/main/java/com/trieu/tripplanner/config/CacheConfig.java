package com.trieu.tripplanner.config;

import com.trieu.tripplanner.common.constant.CacheNames;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
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
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

    /** Places rarely move or change their name. */
    static final Duration PLACE_SEARCH_TTL = Duration.ofHours(24);

    @Bean
    RedisCacheManagerBuilderCustomizer declaredCaches(ObjectMapper objectMapper) {
        return builder -> builder
                .withCacheConfiguration(CacheNames.PLACE_SEARCH,
                        listCache(PLACE_SEARCH_TTL, PlaceResult.class, objectMapper))
                // A typo in a cache name must fail, not create a cache that never expires
                .disableCreateOnMissingCache();
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
