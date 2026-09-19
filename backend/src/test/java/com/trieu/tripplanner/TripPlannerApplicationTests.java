package com.trieu.tripplanner;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripPlannerApplicationTests {

	@Test
	void appliesAllFlywayMigrations(@Autowired Flyway flyway) {
		assertThat(flyway.info().applied())
				.extracting(migration -> migration.getVersion().getVersion())
				.contains("1", "2");
	}

	@Test
	void healthEndpointReportsUp(@Autowired MockMvcTester mvc) {
		assertThat(mvc.get().uri("/actuator/health"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.status").isEqualTo("UP");
	}

	@Test
	void actuatorEndpointsOutsideWhitelistAreNotExposed(@Autowired MockMvcTester mvc) {
		assertThat(mvc.get().uri("/actuator/env"))
				.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void unknownApiPathReturnsErrorEnvelope(@Autowired MockMvcTester mvc) {
		assertThat(mvc.get().uri("/api/v1/khong-ton-tai"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson()
				.extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
	}

	@Test
	void openApiDocumentsPingEndpoint(@Autowired MockMvcTester mvc) {
		assertThat(mvc.get().uri("/v3/api-docs"))
				.hasStatusOk()
				.bodyJson()
				.hasPath("$.paths['/api/v1/ping'].get");
	}

}