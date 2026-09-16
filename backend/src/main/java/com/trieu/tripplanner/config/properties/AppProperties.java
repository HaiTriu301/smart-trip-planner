package com.trieu.tripplanner.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Application-level settings bound from the {@code app.*} prefix.
 * Validated at startup so a missing or misspelled value fails fast instead of at first use.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
		@NotBlank @URL String frontendUrl,
		@NotNull @Valid Providers providers) {

	/**
	 * Which implementation backs each third-party port (design.md 7.1).
	 */
	public record Providers(
			@NotBlank @Pattern(regexp = "mock|osm") String map,
			@NotBlank @Pattern(regexp = "mock|open-meteo") String weather,
			@NotBlank @Pattern(regexp = "mock|stripe") String payment,
			@NotBlank @Pattern(regexp = "mock|claude") String ai,
			@NotBlank @Pattern(regexp = "local|cloudinary") String storage) {
	}

}