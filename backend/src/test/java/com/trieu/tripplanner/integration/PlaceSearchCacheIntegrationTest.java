package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.common.constant.CacheNames;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import com.trieu.tripplanner.service.PlaceSearchCache;
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
 * The place search cache against a real Redis (design.md 8.1 "place:search"), with the real mock map source
 * behind it. What only this level can prove:
 * <ul>
 *   <li>an answer really lands in Redis, under the expected key, with a 24 hour lifetime;</li>
 *   <li>the next identical question is answered from Redis and not by the source;</li>
 *   <li>what comes back from Redis is the same list of the same records, not a list of maps.</li>
 * </ul>
 * "Answered from Redis" is proven without counting calls: the stored answer is swapped for a made-up one, and
 * the next search must return the made-up one.
 * <p>
 * Same annotations as the other flow tests, so the application context (and its Redis) is shared with them:
 * the cache is emptied before and after every test here, so nothing made up leaks into another test class.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PlaceSearchCacheIntegrationTest {

    /** How Spring names the entry in Redis: the cache name, two colons, then our key. */
    private static final String REDIS_KEY_OF_CHO_HAN = "place:search::MOCK|8|-|chợ hàn";

    private static final PlaceResult MADE_UP = new PlaceResult(PlaceProvider.MOCK, "made-up", "Địa điểm bịa ra",
            null, new BigDecimal("1.0000000"), new BigDecimal("2.0000000"), null);

    @Autowired
    private PlaceSearchCache placeSearchCache;

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
    void firstSearchStoresTheAnswerInRedisForTwentyFourHours() {
        assertThat(redis.hasKey(REDIS_KEY_OF_CHO_HAN)).isFalse();

        List<PlaceResult> answer = placeSearchCache.search("chợ hàn", 8, null);

        assertThat(answer).extracting(PlaceResult::name).contains("Chợ Hàn");
        assertThat(redis.hasKey(REDIS_KEY_OF_CHO_HAN)).isTrue();
        long secondsLeft = redis.getExpire(REDIS_KEY_OF_CHO_HAN, TimeUnit.SECONDS);
        assertThat(secondsLeft).isBetween(TimeUnit.HOURS.toSeconds(24) - 60, TimeUnit.HOURS.toSeconds(24));
        // Plain JSON, readable by a person, and no Java class name in it
        String stored = redis.opsForValue().get(REDIS_KEY_OF_CHO_HAN);
        assertThat(stored).startsWith("[{").contains("\"name\":\"Chợ Hàn\"").doesNotContain("com.trieu", "@class");
    }

    @Test
    void sameQuestionIsThenAnsweredFromRedisNotByTheSource() {
        placeSearchCache.search("chợ hàn", 8, null);
        // Swap the stored answer: if the next search asked the source, the real places would come back
        cache().put(placeSearchCache.keyOf("chợ hàn", 8, null), List.of(MADE_UP));

        assertThat(placeSearchCache.search("chợ hàn", 8, null)).containsExactly(MADE_UP);
        // Typed in capitals, with extra spaces: still the same question
        assertThat(placeSearchCache.search("  CHỢ   HÀN ", 8, null)).containsExactly(MADE_UP);
    }

    @Test
    void anotherQuestionIsAskedToTheSource() {
        placeSearchCache.search("chợ hàn", 8, null);
        cache().put(placeSearchCache.keyOf("chợ hàn", 8, null), List.of(MADE_UP));

        // The same words without accents are another keyword to a real map source (Task 3.8)
        assertThat(placeSearchCache.search("cho han", 8, null)).doesNotContain(MADE_UP)
                .extracting(PlaceResult::name).contains("Chợ Hàn");
        assertThat(redis.hasKey("place:search::MOCK|8|-|cho han")).isTrue();
        // Another keyword, another limit, another reference point: none of them may get the stored answer
        assertThat(placeSearchCache.search("chợ cồn", 8, null)).doesNotContain(MADE_UP)
                .extracting(PlaceResult::name).contains("Chợ Cồn");
        assertThat(placeSearchCache.search("chợ hàn", 20, null)).doesNotContain(MADE_UP)
                .extracting(PlaceResult::name).contains("Chợ Hàn");
        assertThat(placeSearchCache.search("chợ hàn", 8, daNang())).doesNotContain(MADE_UP)
                .extracting(PlaceResult::name).contains("Chợ Hàn");
    }

    @Test
    void answerReadBackFromRedisIsTheSameListOfTheSameRecords() {
        List<PlaceResult> fromTheSource = placeSearchCache.search("da nang", 20, daNang());
        assertThat(fromTheSource).hasSizeGreaterThan(5);

        List<PlaceResult> fromRedis = placeSearchCache.search("da nang", 20, daNang());

        // Same places in the same order, every field equal, coordinates to the last decimal; and real records:
        // a list of maps would make the next line fail with a ClassCastException
        assertThat(fromRedis).isEqualTo(fromTheSource);
        assertThat(fromRedis.getFirst().provider()).isEqualTo(PlaceProvider.MOCK);
    }

    @Test
    void emptyAnswerIsStoredToo() {
        assertThat(placeSearchCache.search("khong co dia diem nao ten nay", 8, null)).isEmpty();

        assertThat(redis.opsForValue().get("place:search::MOCK|8|-|khong co dia diem nao ten nay")).isEqualTo("[]");
        assertThat(placeSearchCache.search("khong co dia diem nao ten nay", 8, null)).isEmpty();
    }

    @Test
    void storedAnswerIsInRedisTheMomentTheSearchReturns() {
        // Many different questions in a row: with writes handed to a background task, some of these reads
        // ran ahead of their write and found nothing (BUG-PLACE-003, red in about one run out of five)
        for (int i = 0; i < 40; i++) {
            String keyword = "khong co ket qua so " + i;
            placeSearchCache.search(keyword, 8, null);
            assertThat(redis.hasKey("place:search::" + placeSearchCache.keyOf(keyword, 8, null)))
                    .as("entry of search %d", i).isTrue();
        }
    }

    @Test
    void cacheThatWasNotDeclaredDoesNotExist() {
        // A typo in a cache name must not quietly create a cache without a lifetime
        assertThat(cacheManager.getCacheNames()).contains(CacheNames.PLACE_SEARCH);
        assertThat(cacheManager.getCache("place:serach")).isNull();
    }

    private Cache cache() {
        return cacheManager.getCache(CacheNames.PLACE_SEARCH);
    }

    private static Coordinate daNang() {
        return new Coordinate(new BigDecimal("16.0678000"), new BigDecimal("108.2208000"));
    }

}
