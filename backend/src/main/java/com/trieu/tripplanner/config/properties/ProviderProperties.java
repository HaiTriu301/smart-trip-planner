package com.trieu.tripplanner.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * How the real third-party services are reached, bound from {@code app.providers.*} (design.md 7.2). Which
 * implementation is active stays in {@link AppProperties.Providers}; this record only holds what a real
 * implementation needs to make its calls. Addresses are configuration so that tests point at a stand-in server
 * and a self-hosted instance can replace a public one without a code change.
 *
 * @param userAgent sent with every outgoing call: the public services ask applications to say who they are
 * @param openMeteo the weather forecast service
 * @param osm       the OpenStreetMap services behind the map source
 */
@Validated
@ConfigurationProperties(prefix = "app.providers")
public record ProviderProperties(
        @NotBlank String userAgent,
        @NotNull @Valid OpenMeteo openMeteo,
        @NotNull @Valid Osm osm) {

    /**
     * @param baseUrl scheme and host of the forecast API, without a path
     */
    public record OpenMeteo(@NotBlank @URL String baseUrl) {
    }

    /**
     * Scheme and host of each service, without a path.
     *
     * @param photonBaseUrl    search while typing
     * @param nominatimBaseUrl one place by its OpenStreetMap id
     * @param osrmBaseUrl      travel by road between the stops of a day
     */
    public record Osm(
            @NotBlank @URL String photonBaseUrl,
            @NotBlank @URL String nominatimBaseUrl,
            @NotBlank @URL String osrmBaseUrl) {
    }

}
