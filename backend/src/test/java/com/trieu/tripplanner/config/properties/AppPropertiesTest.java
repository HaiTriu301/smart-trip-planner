package com.trieu.tripplanner.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AppPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(PropertiesConfig.class)
			.withPropertyValues(
					"app.frontend-url=http://localhost:5173",
					"app.providers.map=mock",
					"app.providers.weather=open-meteo",
					"app.providers.payment=mock",
					"app.providers.ai=mock",
					"app.providers.storage=local");

	@Test
	void bindsKebabCasePropertiesIntoRecord() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			AppProperties properties = context.getBean(AppProperties.class);
			assertThat(properties.frontendUrl()).isEqualTo("http://localhost:5173");
			assertThat(properties.providers().weather()).isEqualTo("open-meteo");
			assertThat(properties.providers().storage()).isEqualTo("local");
		});
	}

	@Test
	void mailProviderAndSenderHaveSafeDefaults() {
		// Neither app.providers.mail nor app.mail-from set above: mock + a placeholder sender
		contextRunner.run(context -> {
			AppProperties properties = context.getBean(AppProperties.class);
			assertThat(properties.providers().mail()).isEqualTo("mock");
			assertThat(properties.mailFrom()).isEqualTo("no-reply@smart-trip-planner.local");
		});
	}

	@Test
	void bindsSmtpMailProviderAndCustomSender() {
		contextRunner
				.withPropertyValues("app.providers.mail=smtp", "app.mail-from=hello@trip.vn")
				.run(context -> {
					AppProperties properties = context.getBean(AppProperties.class);
					assertThat(properties.providers().mail()).isEqualTo("smtp");
					assertThat(properties.mailFrom()).isEqualTo("hello@trip.vn");
				});
	}

	@Test
	void failsStartupWhenFrontendUrlIsBlank() {
		contextRunner
				.withPropertyValues("app.frontend-url=")
				.run(context -> assertThat(context.getStartupFailure())
						.rootCause()
						.isInstanceOf(BindValidationException.class)
						.hasMessageContaining("frontendUrl"));
	}

	@Test
	void failsStartupWhenProviderValueIsUnknown() {
		contextRunner
				.withPropertyValues("app.providers.weather=openmeteo")
				.run(context -> assertThat(context.getStartupFailure())
						.rootCause()
						.isInstanceOf(BindValidationException.class)
						.hasMessageContaining("weather"));
	}

	@Test
	void failsStartupWhenMailProviderIsUnknown() {
		contextRunner
				.withPropertyValues("app.providers.mail=sendgrid")
				.run(context -> assertThat(context.getStartupFailure())
						.rootCause()
						.isInstanceOf(BindValidationException.class)
						.hasMessageContaining("mail"));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(AppProperties.class)
	static class PropertiesConfig {
	}

}
