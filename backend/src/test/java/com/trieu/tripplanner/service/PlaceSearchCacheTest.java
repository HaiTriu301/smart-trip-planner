package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.provider.map.dto.Coordinate;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * The key decides who shares a stored answer with whom: too loose and a user gets the answer to another
 * question, too strict and nobody ever reuses anything.
 */
class PlaceSearchCacheTest {

    private static final Coordinate DA_NANG = point("16.0678", "108.2208");
    private static final Coordinate HA_NOI = point("21.0283", "105.8542");

    @Test
    void keyShowsTheLimitThePointAndTheKeywordInReadableForm() {
        // Readable on purpose: this is what one sees when looking into Redis
        assertThat(PlaceSearchCache.keyOf("Chợ Hàn", 8, DA_NANG)).isEqualTo("8|16.0678,108.2208|cho han");
        assertThat(PlaceSearchCache.keyOf("Chợ Hàn", 8, null)).isEqualTo("8|-|cho han");
    }

    @Test
    void sameKeywordTypedDifferentlyIsOneQuestion() {
        String key = PlaceSearchCache.keyOf("chợ hàn", 8, null);

        assertThat(PlaceSearchCache.keyOf("cho han", 8, null)).isEqualTo(key);          // no accents
        assertThat(PlaceSearchCache.keyOf("CHỢ HÀN", 8, null)).isEqualTo(key);          // capitals
        assertThat(PlaceSearchCache.keyOf("  chợ   hàn ", 8, null)).isEqualTo(key);     // extra spaces
        assertThat(PlaceSearchCache.keyOf("Đà Nẵng", 8, null))
                .isEqualTo(PlaceSearchCache.keyOf("da nang", 8, null));                 // Đ is its own letter
    }

    @Test
    void anotherKeywordAnotherLimitOrAnotherPointIsAnotherQuestion() {
        String key = PlaceSearchCache.keyOf("cho", 8, DA_NANG);

        assertThat(PlaceSearchCache.keyOf("cho han", 8, DA_NANG)).isNotEqualTo(key);
        // A longer list is not the same answer as a shorter one
        assertThat(PlaceSearchCache.keyOf("cho", 20, DA_NANG)).isNotEqualTo(key);
        // The point changes the order of the results
        assertThat(PlaceSearchCache.keyOf("cho", 8, HA_NOI)).isNotEqualTo(key);
        assertThat(PlaceSearchCache.keyOf("cho", 8, null)).isNotEqualTo(key);
    }

    @Test
    void pointsLessThanElevenMetresApartAreOnePoint() {
        String key = PlaceSearchCache.keyOf("cho", 8, DA_NANG);

        // Both round to 16.0678, 108.2208
        assertThat(PlaceSearchCache.keyOf("cho", 8, point("16.06784", "108.22076"))).isEqualTo(key);
        // The same number as MySQL hands it back, with seven decimals
        assertThat(PlaceSearchCache.keyOf("cho", 8, point("16.0678000", "108.2208000"))).isEqualTo(key);
        // The fourth decimal differs: another point
        assertThat(PlaceSearchCache.keyOf("cho", 8, point("16.0679", "108.2208"))).isNotEqualTo(key);
    }

    @Test
    void keywordThatLooksLikeAKeyCannotPassForAnotherSearch() {
        // Typed by a user: the text of another key, separators included
        String forged = PlaceSearchCache.keyOf("8|-|cho han", 8, null);

        assertThat(forged).isNotEqualTo(PlaceSearchCache.keyOf("cho han", 8, null));
        // Limit 8 with keyword "9|..." must not meet limit 89
        assertThat(PlaceSearchCache.keyOf("9|-|cho", 8, null)).isNotEqualTo(PlaceSearchCache.keyOf("cho", 89, null));
    }

    private static Coordinate point(String lat, String lng) {
        return new Coordinate(new BigDecimal(lat), new BigDecimal(lng));
    }

}
