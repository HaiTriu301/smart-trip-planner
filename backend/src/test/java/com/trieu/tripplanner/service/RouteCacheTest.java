package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Which route questions share one stored answer. Only the key is checked here, no Spring and no Redis; that the
 * key is really what Redis stores under is RouteCacheIntegrationTest.
 */
class RouteCacheTest {

    private static final Coordinate CHO_HAN = point("16.0683525", "108.2242830");
    private static final Coordinate CAU_RONG = point("16.0611370", "108.2275720");
    private static final Coordinate LINH_UNG = point("16.1001567", "108.2784112");

    @Test
    void keyHoldsTheSourceAndThePointsInTheOrderTheyAreTravelled() {
        assertThat(RouteCache.keyOf(PlaceProvider.OSM, List.of(CHO_HAN, CAU_RONG, LINH_UNG)))
                .isEqualTo("OSM|16.06835,108.22428;16.06114,108.22757;16.10016,108.27841");
    }

    @Test
    void samePointsInAnotherOrderAreAnotherQuestion() {
        assertThat(RouteCache.keyOf(PlaceProvider.OSM, List.of(CHO_HAN, CAU_RONG, LINH_UNG)))
                .isNotEqualTo(RouteCache.keyOf(PlaceProvider.OSM, List.of(CHO_HAN, LINH_UNG, CAU_RONG)))
                .isNotEqualTo(RouteCache.keyOf(PlaceProvider.OSM, List.of(LINH_UNG, CAU_RONG, CHO_HAN)));
    }

    @Test
    void onePointMoreOrFewerIsAnotherQuestion() {
        assertThat(RouteCache.keyOf(PlaceProvider.OSM, List.of(CHO_HAN, CAU_RONG)))
                .isNotEqualTo(RouteCache.keyOf(PlaceProvider.OSM, List.of(CHO_HAN, CAU_RONG, LINH_UNG)))
                // The same stop twice in a row is a leg of its own
                .isNotEqualTo(RouteCache.keyOf(PlaceProvider.OSM, List.of(CHO_HAN, CHO_HAN, CAU_RONG)));
    }

    @Test
    void pointsLessThanAMetreApartAreTheSameStop() {
        // The sixth decimal differs; both round to 16.06835 / 108.22428
        Coordinate nextToChoHan = point("16.0683549", "108.2242751");

        assertThat(RouteCache.keyOf(PlaceProvider.OSM, List.of(nextToChoHan, CAU_RONG)))
                .isEqualTo(RouteCache.keyOf(PlaceProvider.OSM, List.of(CHO_HAN, CAU_RONG)));
    }

    @Test
    void pointsAFewMetresApartAreDifferentStops() {
        // About 5 m to the north
        Coordinate nearChoHan = point("16.0684000", "108.2242830");

        assertThat(RouteCache.keyOf(PlaceProvider.OSM, List.of(nearChoHan, CAU_RONG)))
                .isNotEqualTo(RouteCache.keyOf(PlaceProvider.OSM, List.of(CHO_HAN, CAU_RONG)));
    }

    @Test
    void samePointsAskedToAnotherSourceAreAnotherQuestion() {
        // A straight-line estimate must not be served as the answer of a routing service, nor the other way round
        assertThat(RouteCache.keyOf(PlaceProvider.MOCK, List.of(CHO_HAN, CAU_RONG)))
                .isEqualTo("MOCK|16.06835,108.22428;16.06114,108.22757")
                .isNotEqualTo(RouteCache.keyOf(PlaceProvider.OSM, List.of(CHO_HAN, CAU_RONG)));
    }

    @Test
    void keyOfTheBeanNamesTheSourceInUse() {
        MapProvider osm = mock(MapProvider.class);
        when(osm.provider()).thenReturn(PlaceProvider.OSM);

        assertThat(new RouteCache(osm).keyOf(List.of(CHO_HAN, CAU_RONG)))
                .isEqualTo("OSM|16.06835,108.22428;16.06114,108.22757");
    }

    private static Coordinate point(String lat, String lng) {
        return new Coordinate(new BigDecimal(lat), new BigDecimal(lng));
    }

}
