package com.trieu.tripplanner.provider.map.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CoordinateTest {

    private static final Coordinate HA_NOI = point("21.0285", "105.8542");
    private static final Coordinate HO_CHI_MINH = point("10.7769", "106.7009");
    private static final Coordinate CHO_HAN = point("16.0683525", "108.2242830");
    private static final Coordinate CAU_RONG = point("16.0611682", "108.2278968");

    @Test
    void distanceBetweenTwoCitiesIsTheStraightLineOverTheGlobe() {
        // About 1 144 km as the crow flies; the road is much longer
        assertThat(HA_NOI.distanceMetersTo(HO_CHI_MINH)).isCloseTo(1_143_500, within(1_000.0));
    }

    @Test
    void distanceInsideACityIsAccurateToAFewMetres() {
        assertThat(CHO_HAN.distanceMetersTo(CAU_RONG)).isCloseTo(887, within(5.0));
    }

    @Test
    void oneDegreeOfLatitudeIsAbout111Kilometres() {
        assertThat(point("0", "0").distanceMetersTo(point("1", "0"))).isCloseTo(111_195, within(10.0));
    }

    @Test
    void distanceIsTheSameInBothDirectionsAndZeroToItself() {
        assertThat(HA_NOI.distanceMetersTo(HO_CHI_MINH)).isEqualTo(HO_CHI_MINH.distanceMetersTo(HA_NOI));
        assertThat(CHO_HAN.distanceMetersTo(CHO_HAN)).isZero();
    }

    private static Coordinate point(String lat, String lng) {
        return new Coordinate(new BigDecimal(lat), new BigDecimal(lng));
    }

}
