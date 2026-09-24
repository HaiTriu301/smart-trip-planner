package com.trieu.tripplanner.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Application-level settings bound from the {@code app.*} prefix.
 * Validated at startup so a missing or misspelled value fails fast instead of at first use.
 *
 * @param frontendUrl origin allowed by CORS and base of every link put in an email
 * @param mailFrom    sender address of outgoing mail
 * @param providers   which implementation backs each third-party port
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
		@NotBlank @URL String frontendUrl,
		@DefaultValue("no-reply@smart-trip-planner.local") @NotBlank @Email String mailFrom,
		@NotNull @Valid Providers providers) {

	/**
	 * Which implementation backs each third-party port (design.md 7.1).
	 * {@code mail} defaults to mock so tests and key-less environments never open an SMTP connection.
	 */
	public record Providers(
			@NotBlank @Pattern(regexp = "mock|osm") String map,
			@NotBlank @Pattern(regexp = "mock|open-meteo") String weather,
			@NotBlank @Pattern(regexp = "mock|stripe") String payment,
			@NotBlank @Pattern(regexp = "mock|claude") String ai,
			@NotBlank @Pattern(regexp = "local|cloudinary") String storage,
			@DefaultValue("mock") @NotBlank @Pattern(regexp = "mock|smtp") String mail) {
	}

}
