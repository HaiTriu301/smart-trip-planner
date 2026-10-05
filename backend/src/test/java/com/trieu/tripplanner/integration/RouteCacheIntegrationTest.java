package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.common.constant.CacheNames;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.RouteLeg;
import com.trieu.tripplanner.service.RouteCache;
import java.math.BigDecimal;
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
 * The route cache against a real Redis (design.md 8.1 "route:legs"), with the mock map source behind it (the
 * straight-line estimate). Same approach and same sharing of the application context as
 * PlaceSearchCacheIntegrationTest: "answered from Redis" is proven by swapping the stored answer for a made-up
 * one.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RouteCacheIntegrationTest {

    private static final Coordinate CHO_HAN = point("16.0683525", "108.2242830");
    private static final Coordinate CAU_RONG = point("16.0611370", "108.2275720");
    private static final Coordinate LINH_UNG = point("16.1001567", "108.2784112");

    /** How Spring names the entry in Redis: the cache name, two colons, then our key. */
    private static final String REDIS_KEY = "route:legs::MOCK|16.06835,108.22428;16.06114,108.22757";

    /** 999 km in one second: nothing the source would ever say. */
    private static final RouteLeg MADE_UP = new RouteLeg(999_000, 1);

    @Autowired
    private RouteCache routeCache;

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
    void firstQuestionStoresTheLegsInRedisForTwentyFourHours() {
        assertThat(redis.hasKey(REDIS_KEY)).isFalse();

        List<RouteLeg> legs = routeCache.legs(List.of(CHO_HAN, CAU_RONG));

        assertThat(legs).hasSize(1);
        assertThat(redis.hasKey(REDIS_KEY)).isTrue();
        long secondsLeft = redis.getExpire(REDIS_KEY, TimeUnit.SECONDS);
        assertThat(secondsLeft).isBetween(TimeUnit.HOURS.toSeconds(24) - 60, TimeUnit.HOURS.toSeconds(24));
        // Plain JSON, readable by a person, and no Java class name in it
        assertThat(redis.opsForValue().get(REDIS_KEY))
                .isEqualTo("[{\"distanceMeters\":%d,\"durationSeconds\":%d}]"
                        .formatted(legs.getFirst().distanceMeters(), legs.getFirst().durationSeconds()))
                .doesNotContain("com.trieu", "@class");
    }

    @Test
    void samePointsInTheSameOrderAreThenAnsweredFromRedisNotByTheSource() {
        routeCache.legs(List.of(CHO_HAN, CAU_RONG));
        // Swap the stored answer: if the next question reached the source, the real leg would come back
        cache().put(routeCache.keyOf(List.of(CHO_HAN, CAU_RONG)), List.of(MADE_UP));

        assertThat(routeCache.legs(List.of(CHO_HAN, CAU_RONG))).containsExactly(MADE_UP);
        // Less than a metre away: the same stop
        assertThat(routeCache.legs(List.of(point("16.0683549", "108.2242751"), CAU_RONG))).containsExactly(MADE_UP);
    }

    @Test
    void anotherOrderAnotherPointOrOnePointMoreIsAskedToTheSource() {
        List<RouteLeg> real = routeCache.legs(List.of(CHO_HAN, CAU_RONG));
        cache().put(routeCache.keyOf(List.of(CHO_HAN, CAU_RONG)), List.of(MADE_UP));

        // The way back: the same two points, the other order
        assertThat(routeCache.legs(List.of(CAU_RONG, CHO_HAN))).isEqualTo(real);
        assertThat(routeCache.legs(List.of(CHO_HAN, LINH_UNG))).doesNotContain(MADE_UP).hasSize(1);
        // A day that goes on after the two stored points is a question of its own, not the stored one plus a leg
        assertThat(routeCache.legs(List.of(CHO_HAN, CAU_RONG, LINH_UNG))).doesNotContain(MADE_UP).hasSize(2);
        assertThat(redis.hasKey("route:legs::MOCK|16.06114,108.22757;16.06835,108.22428")).isTrue();
    }

    @Test
    void legsReadBackFromRedisAreTheSameListOfTheSameRecords() {
        List<Coordinate> day = List.of(CHO_HAN, CAU_RONG, CAU_RONG, LINH_UNG);
        List<RouteLeg> fromTheSource = routeCache.legs(day);
        assertThat(fromTheSource).hasSize(3);

        List<RouteLeg> fromRedis = routeCache.legs(day);

        // Same numbers in the same order, the leg of zero included, and real records, not maps
        assertThat(fromRedis).isEqualTo(fromTheSource);
        assertThat(fromRedis.get(1)).isEqualTo(new RouteLeg(0, 0));
    }

    private Cache cache() {
        return cacheManager.getCache(CacheNames.ROUTE_LEGS);
    }

    private static Coordinate point(String lat, String lng) {
        return new Coordinate(new BigDecimal(lat), new BigDecimal(lng));
    }

}
