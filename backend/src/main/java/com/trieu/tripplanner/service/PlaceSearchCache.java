package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.constant.CacheNames;
import com.trieu.tripplanner.common.util.VietnameseText;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * Which searches are the same question, and therefore share one stored answer. "Chợ  Hàn", "cho han" and
     * " CHỢ HÀN " are one question: accents, capitals and extra spaces are dropped. The number of results
     * wanted and the reference point are part of the question, because both change the answer.
     * <p>
     * The keyword comes last: whatever the user types, it cannot be read as another limit or another point.
     *
     * @param near the point results are ranked around, or null when there is none
     * @return for example {@code 8|16.0678,108.2208|cho han}, or {@code 8|-|cho han} without a point
     */
    public static String keyOf(String query, int limit, Coordinate near) {
        String keyword = VietnameseText.stripAccents(query).toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
        String point = (near == null) ? NO_COORDINATE : rounded(near.lat()) + "," + rounded(near.lng());
        return limit + "|" + point + "|" + keyword;
    }

    private static String rounded(BigDecimal degrees) {
        return degrees.setScale(COORDINATE_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

}
