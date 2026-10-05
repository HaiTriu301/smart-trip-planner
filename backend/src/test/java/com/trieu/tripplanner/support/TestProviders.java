package com.trieu.tripplanner.support;

import com.trieu.tripplanner.config.properties.ProviderProperties;

/**
 * Builds the settings of the real providers for tests that construct a provider by hand. One place knows the
 * constructor of {@link ProviderProperties}: adding a service changes this class, not every provider test.
 * A service the test does not use gets an address that leads nowhere.
 */
public final class TestProviders {

    private static final String NOT_USED = "http://not-used.invalid";

    private TestProviders() {
    }

    /** Settings for a test of the weather source: Open-Meteo is found at {@code baseUrl}. */
    public static ProviderProperties openMeteoAt(String userAgent, String baseUrl) {
        return new ProviderProperties(userAgent,
                new ProviderProperties.OpenMeteo(baseUrl),
                new ProviderProperties.Osm(NOT_USED, NOT_USED, NOT_USED));
    }

    /** Settings for a test of the map source: Photon, Nominatim and OSRM are found at the three addresses. */
    public static ProviderProperties osmAt(String userAgent, String photonBaseUrl, String nominatimBaseUrl,
            String osrmBaseUrl) {
        return new ProviderProperties(userAgent,
                new ProviderProperties.OpenMeteo(NOT_USED),
                new ProviderProperties.Osm(photonBaseUrl, nominatimBaseUrl, osrmBaseUrl));
    }

}
