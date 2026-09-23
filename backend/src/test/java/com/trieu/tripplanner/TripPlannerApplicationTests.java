package com.trieu.tripplanner;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
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
				.contains("1", "2", "3");
	}

	@Test
	void healthEndpointReportsUp(@Autowired MockMvcTester mvc) {
		assertThat(mvc.get().uri("/actuator/health"))
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.status").isEqualTo("UP");
	}

	@Test
	@WithMockUser
	void actuatorEndpointsOutsideWhitelistAreNotExposed(@Autowired MockMvcTester mvc) {
		// Logged in so we get past security and prove the endpoint itself is not registered
		assertThat(mvc.get().uri("/actuator/env"))
				.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	@WithMockUser
	void unknownApiPathReturnsNotFoundEnvelopeForLoggedInUser(@Autowired MockMvcTester mvc) {
		assertThat(mvc.get().uri("/api/v1/khong-ton-tai"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson()
				.extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
	}

	@Test
	void unknownApiPathReturnsUnauthorizedEnvelopeForAnonymous(@Autowired MockMvcTester mvc) {
		// Since Task 1.2 the filter chain answers before routing: anonymous callers get 401, never 404
		assertThat(mvc.get().uri("/api/v1/khong-ton-tai"))
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.bodyJson()
				.extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
	}

	@Test
	void openApiDocumentsPingEndpoint(@Autowired MockMvcTester mvc) {
		assertThat(mvc.get().uri("/v3/api-docs"))
				.hasStatusOk()
				.bodyJson()
				.hasPath("$.paths['/api/v1/ping'].get");
	}

}