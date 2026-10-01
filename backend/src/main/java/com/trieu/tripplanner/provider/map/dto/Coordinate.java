package com.trieu.tripplanner.provider.map.dto;

import java.math.BigDecimal;

/**
 * A point on the map. Latitude -90..90, longitude -180..180, as decimal degrees.
 */
public record Coordinate(BigDecimal lat, BigDecimal lng) {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    /**
     * Straight-line distance over the surface of the globe ("as the crow flies"), haversine formula.
     * Good enough to tell near from far and to sort by closeness; it is not a travel distance.
     */
    public double distanceMetersTo(Coordinate other) {
        double fromLat = Math.toRadians(lat.doubleValue());
        double toLat = Math.toRadians(other.lat.doubleValue());
        double deltaLat = toLat - fromLat;
        double deltaLng = Math.toRadians(other.lng.doubleValue() - lng.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(fromLat) * Math.cos(toLat) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a));
    }

}
