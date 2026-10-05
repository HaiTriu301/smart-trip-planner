package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import java.math.BigDecimal;
import java.text.Normalizer;
import org.junit.jupiter.api.Test;

/**
 * The key decides who shares a stored answer with whom: too loose and a user gets the answer to another
 * question, too strict and nobody ever reuses anything.
 */
class PlaceSearchCacheTest {

    private static final Coordinate DA_NANG = point("16.0678", "108.2208");
    private static final Coordinate HA_NOI = point("21.0283", "105.8542");

    @Test
    void keyShowsTheSourceTheLimitThePointAndTheKeywordInReadableForm() {
        // Readable on purpose: this is what one sees when looking into Redis
        assertThat(keyOf("Chợ Hàn", 8, DA_NANG)).isEqualTo("MOCK|8|16.0678,108.2208|chợ hàn");
        assertThat(keyOf("Chợ Hàn", 8, null)).isEqualTo("MOCK|8|-|chợ hàn");
    }

    @Test
    void sameSearchAskedToAnotherSourceIsAnotherQuestion() {
        // A result of one source cannot be picked while the other is in use (BUG-PLACE-004)
        assertThat(PlaceSearchCache.keyOf(PlaceProvider.OSM, "chợ hàn", 8, DA_NANG))
                .isEqualTo("OSM|8|16.0678,108.2208|chợ hàn")
                .isNotEqualTo(PlaceSearchCache.keyOf(PlaceProvider.MOCK, "chợ hàn", 8, DA_NANG));
    }

    @Test
    void keyOfTheBeanNamesTheSourceInUse() {
        MapProvider osm = mock(MapProvider.class);
        when(osm.provider()).thenReturn(PlaceProvider.OSM);

        assertThat(new PlaceSearchCache(osm).keyOf("Chợ Hàn", 8, null)).isEqualTo("OSM|8|-|chợ hàn");
    }

    @Test
    void sameKeywordTypedDifferentlyIsOneQuestion() {
        String key = keyOf("chợ hàn", 8, null);

        assertThat(keyOf("CHỢ HÀN", 8, null)).isEqualTo(key);          // capitals
        assertThat(keyOf("  chợ   hàn ", 8, null)).isEqualTo(key);     // extra spaces
        assertThat(keyOf("ĐÀ NẴNG", 8, null))
                .isEqualTo(keyOf("đà nẵng", 8, null));                 // Đ is its own letter
    }

    @Test
    void keywordWithAndWithoutAccentsAreTwoQuestions() {
        // A real map source answers them differently: one must not get the stored answer of the other
        assertThat(keyOf("cho han", 8, null)).isNotEqualTo(keyOf("chợ hàn", 8, null));
        assertThat(keyOf("da nang", 8, null)).isNotEqualTo(keyOf("đà nẵng", 8, null));
        // Another accent is another word: market / wait
        assertThat(keyOf("chờ", 8, null)).isNotEqualTo(keyOf("chợ", 8, null));
    }

    @Test
    void sameLetterSentAsOneCharacterOrAsLetterPlusAccentMarksIsOneQuestion() {
        // Some keyboards send "ợ" as o + horn + dot below; it looks the same on screen
        String oneCharacterPerLetter = Normalizer.normalize("chợ hàn", Normalizer.Form.NFC);
        String letterPlusMarks = Normalizer.normalize("chợ hàn", Normalizer.Form.NFD);
        assertThat(letterPlusMarks).isNotEqualTo(oneCharacterPerLetter);

        assertThat(keyOf(letterPlusMarks, 8, null))
                .isEqualTo(keyOf(oneCharacterPerLetter, 8, null));
    }

    @Test
    void anotherKeywordAnotherLimitOrAnotherPointIsAnotherQuestion() {
        String key = keyOf("cho", 8, DA_NANG);

        assertThat(keyOf("cho han", 8, DA_NANG)).isNotEqualTo(key);
        // A longer list is not the same answer as a shorter one
        assertThat(keyOf("cho", 20, DA_NANG)).isNotEqualTo(key);
        // The point changes the order of the results
        assertThat(keyOf("cho", 8, HA_NOI)).isNotEqualTo(key);
        assertThat(keyOf("cho", 8, null)).isNotEqualTo(key);
    }

    @Test
    void pointsLessThanElevenMetresApartAreOnePoint() {
        String key = keyOf("cho", 8, DA_NANG);

        // Both round to 16.0678, 108.2208
        assertThat(keyOf("cho", 8, point("16.06784", "108.22076"))).isEqualTo(key);
        // The same number as MySQL hands it back, with seven decimals
        assertThat(keyOf("cho", 8, point("16.0678000", "108.2208000"))).isEqualTo(key);
        // The fourth decimal differs: another point
        assertThat(keyOf("cho", 8, point("16.0679", "108.2208"))).isNotEqualTo(key);
    }

    @Test
    void keywordThatLooksLikeAKeyCannotPassForAnotherSearch() {
        // Typed by a user: the text of another key, separators included
        String forged = keyOf("8|-|cho han", 8, null);

        assertThat(forged).isNotEqualTo(keyOf("cho han", 8, null));
        // Limit 8 with keyword "9|..." must not meet limit 89
        assertThat(keyOf("9|-|cho", 8, null)).isNotEqualTo(keyOf("cho", 89, null));
    }

    /** The key for the bundled source: what these tests compare is the rest of the key. */
    private static String keyOf(String query, int limit, Coordinate near) {
        return PlaceSearchCache.keyOf(PlaceProvider.MOCK, query, limit, near);
    }

    private static Coordinate point(String lat, String lng) {
        return new Coordinate(new BigDecimal(lat), new BigDecimal(lng));
    }

}
