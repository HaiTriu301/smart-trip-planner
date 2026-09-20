package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Full stack against real MySQL: HTTP → validation → service → BCrypt → JPA → Flyway schema.
 * This is the automated form of the Task 1.2 acceptance check ("password_hash starts with $2a$12$").
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthRegistrationIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String BODY = """
            {"email": "IT.User@Example.com", "password": "MatKhau123", "confirmPassword": "MatKhau123", "fullName": "Người dùng IT"}
            """;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        // @SpringBootTest is not transactional, so rows survive between tests unless removed
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void registerPersistsUserWithBcryptHashAndLowercaseEmail() {
        assertThat(mvc.post().uri(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": true,
                          "data": { "email": "it.user@example.com", "fullName": "Người dùng IT",
                                    "role": "USER", "plan": "FREE", "emailVerified": false, "status": "ACTIVE" }
                        }
                        """);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT email, password_hash, email_verified, role, plan, status FROM users WHERE email = ?",
                "it.user@example.com");
        assertThat(row.get("email")).isEqualTo("it.user@example.com");
        assertThat((String) row.get("password_hash")).startsWith("$2a$12$").doesNotContain("MatKhau123");
        assertThat(row.get("email_verified")).isEqualTo(false);
        assertThat(row.get("role")).isEqualTo("USER");
        assertThat(row.get("plan")).isEqualTo("FREE");
        assertThat(row.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    void registeringSameEmailTwiceReturns409AndKeepsOneRow() {
        assertThat(mvc.post().uri(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .hasStatus(HttpStatus.CREATED);

        assertThat(mvc.post().uri(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("EMAIL_ALREADY_EXISTS");

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThat(count).isEqualTo(1);
    }

}
