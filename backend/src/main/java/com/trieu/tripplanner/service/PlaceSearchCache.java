package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.constant.CacheNames;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Temporary copies of place search answers (design.md 8.1 "place:search"). Stands between PlaceService and the
 * map source: the first time a question is asked the source answers and the answer is stored in Redis; for the
 * next 24 hours the same question, from any user, is answered from Redis and the source is left alone.
 * <p>
 * A bean of its own on purpose. The cache works through a proxy that Spring puts in front of this bean, so it
 * only sees calls coming from another bean; and it knows nothing about which map source is in use, so adding a
 * real source changes nothing here.
 */
@Component
@RequiredArgsConstructor
public class PlaceSearchCache {

    /** Two points closer than about 11 m count as the same reference point. */
    static final int COORDINATE_SCALE = 4;

    private static final String NO_COORDINATE = "-";

    private final MapProvider mapProvider;

    /**
     * What the map source answers to this search, from Redis when the same question was asked in the last
     * 24 hours. An empty answer is stored like any other: "nothing found" is not asked again either.
     *
     * @param query keyword, already trimmed
     * @param near  the point results are ranked around, or null when there is none
     */
    @Cacheable(cacheNames = CacheNames.PLACE_SEARCH, key = "#root.target.keyOf(#query, #limit, #near)")
    public List<PlaceResult> search(String query, int limit, Coordinate near) {
        return mapProvider.search(query, limit, near);
    }

    /** The key of this search for the map source in use. */
    public String keyOf(String query, int limit, Coordinate near) {
        return keyOf(mapProvider.provider(), query, limit, near);
    }

    /**
     * Which searches are the same question, and therefore share one stored answer. "Chợ  Hàn" and " CHỢ HÀN "
     * are one question: capitals and extra spaces are dropped. Accents are kept (design.md 8.1, Task 3.8): to a
     * real map source "chợ hàn" and "cho han" are two keywords with two answers, and "cho han" must not get the
     * answer stored for the other. A letter typed as one character or as a base letter followed by its accent
     * marks (as some keyboards send it) is the same letter. The number of results wanted and the reference
     * point are part of the question, because both change the answer.
     * <p>
     * The source is part of the question too: its results carry ids that mean nothing to another source. Without
     * it, a search stored while {@code app.providers.map} had one value was served for a day after a switch to
     * the other, and picking such a result was refused (BUG-PLACE-004).
     * <p>
     * The keyword comes last: whatever the user types, it cannot be read as another limit or another point.
     *
     * @param near the point results are ranked around, or null when there is none
     * @return for example {@code OSM|8|16.0678,108.2208|chợ hàn}, or {@code OSM|8|-|chợ hàn} without a point
     */
    public static String keyOf(PlaceProvider source, String query, int limit, Coordinate near) {
        String keyword = Normalizer.normalize(query, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
        String point = (near == null) ? NO_COORDINATE : rounded(near.lat()) + "," + rounded(near.lng());
        return source + "|" + limit + "|" + point + "|" + keyword;
    }

    private static String rounded(BigDecimal degrees) {
        return degrees.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

}
