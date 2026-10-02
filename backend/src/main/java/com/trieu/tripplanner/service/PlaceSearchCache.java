package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.util.VietnameseText;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Temporary copies of place search answers (design.md 8.1 "place:search").
 */
public final class PlaceSearchCache {

    /** Two points closer than about 11 m count as the same reference point. */
    static final int COORDINATE_SCALE = 4;

    private static final String NO_COORDINATE = "-";

    private PlaceSearchCache() {
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
