package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Trip CRUD through every layer against real MySQL: JWT filter → @PreAuthorize(@tripPermission) → service
 * → Flyway schema. Complements TripControllerTest (mocked service) and TripServiceTest (mocked repository):
 * only here do the real evaluator, soft delete and ownership filtering meet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripFlowIntegrationTest {

    private static final String TRIPS_URL = "/api/v1/trips";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String ownerBearer;
    private String strangerBearer;

    @BeforeEach
    void createUsers() {
        ownerBearer = bearerFor(userRepository.save(user("owner@example.com")));
        strangerBearer = bearerFor(userRepository.save(user("stranger@example.com")));
    }

    @AfterEach
    void cleanUp() {
        // trips first: fk_trips_owner has no ON DELETE CASCADE
        jdbcTemplate.update("DELETE FROM trips");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void ownerCreatesReadsUpdatesAndSoftDeletesATrip() {
        MvcTestResult created = createTrip(ownerBearer, "Đà Lạt 3 ngày", "2026-10-01", "2026-10-03");
        assertThat(created).hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "title": "Đà Lạt 3 ngày", "status": "DRAFT", "visibility": "PRIVATE",
                                    "currency": "VND", "version": 0 } }
                        """);
        long id = idOf(created);
        String slug = JsonPath.read(body(created), "$.data.slug");
        assertThat(slug).matches("da-lat-3-ngay-[a-z0-9]{6}");

        assertThat(mvc.get().uri(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, ownerBearer))
                .hasStatusOk()
                .bodyJson().extractingPath("$.data.slug").isEqualTo(slug);

        assertThat(patch(ownerBearer, id, """
                { "title": "Đà Lạt mùa hoa", "endDate": "2026-10-05" }
                """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "title": "Đà Lạt mùa hoa", "endDate": "2026-10-05", "startDate": "2026-10-01",
                                    "slug": "%s", "version": 1 } }
                        """.formatted(slug));

        assertThat(mvc.delete().uri(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, ownerBearer))
                .hasStatusOk();
        assertThat(mvc.get().uri(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, ownerBearer))
                .hasStatus(HttpStatus.NOT_FOUND);
        // Soft delete: the row survives with deleted_at stamped
        Integer stamped = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trips WHERE id = ? AND deleted_at IS NOT NULL", Integer.class, id);
        assertThat(stamped).isEqualTo(1);
    }

    @Test
    void listShowsOnlyOwnTripsWithPagingAndFilters() {
        createTrip(ownerBearer, "Hà Giang", "2026-09-01", "2026-09-04");
        createTrip(ownerBearer, "Vũng Tàu", "2026-10-10", "2026-10-11");
        createTrip(ownerBearer, "Phú Quốc", "2026-10-30", "2026-11-02");
        createTrip(strangerBearer, "Của người khác", "2026-10-15", "2026-10-16");

        assertThat(list(ownerBearer, "?size=2"))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "data": { "page": 0, "size": 2, "totalElements": 3, "totalPages": 2, "hasNext": true } }
                        """);

        // Overlap with October: Phú Quốc (30/10–02/11) counts, Hà Giang (September) does not
        assertThat(titles(list(ownerBearer, "?from=2026-10-01&to=2026-10-31&sort=startDate,asc")))
                .containsExactly("Vũng Tàu", "Phú Quốc");
        assertThat(titles(list(ownerBearer, "?q=giang"))).containsExactly("Hà Giang");
        assertThat(titles(list(strangerBearer, ""))).containsExactly("Của người khác");
    }

    @Test
    void strangerGets403OnReadEditAndDeleteAndTheTripIsUntouched() {
        long id = idOf(createTrip(ownerBearer, "Riêng tư", "2026-10-01", "2026-10-02"));

        assertThat(mvc.get().uri(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, strangerBearer))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("FORBIDDEN");
        assertThat(patch(strangerBearer, id, """
                { "title": "Bị sửa trộm" }
                """)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.delete().uri(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, strangerBearer))
                .hasStatus(HttpStatus.FORBIDDEN);

        String title = jdbcTemplate.queryForObject(
                "SELECT title FROM trips WHERE id = ? AND deleted_at IS NULL", String.class, id);
        assertThat(title).isEqualTo("Riêng tư");
    }

    @Test
    void missingOrDeletedTripIs404EvenForAStranger() {
        long id = idOf(createTrip(ownerBearer, "Sẽ bị xoá", "2026-10-01", "2026-10-01"));
        assertThat(mvc.delete().uri(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, ownerBearer))
                .hasStatusOk();

        assertThat(mvc.get().uri(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, strangerBearer))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, ownerBearer))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri(TRIPS_URL + "/999999").header(HttpHeaders.AUTHORIZATION, ownerBearer))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void businessRulesAnswer400WithVietnameseFieldDetails() {
        assertThat(createTrip(ownerBearer, "Quá dài", "2026-10-01", "2026-11-30"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "endDate", "message": "Chuyến đi dài tối đa 60 ngày" } ] }
                        """);

        long id = idOf(createTrip(ownerBearer, "Hợp lệ", "2026-10-10", "2026-10-12"));
        assertThat(patch(ownerBearer, id, """
                { "endDate": "2026-10-09" }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "details": [ { "field": "endDate", "message": "Ngày kết thúc phải bằng hoặc sau ngày bắt đầu" } ] }
                        """);

        assertThat(list(ownerBearer, "?sort=slug,asc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.details[0].field").isEqualTo("sort");
    }

    private MvcTestResult createTrip(String bearer, String title, String start, String end) {
        return mvc.post().uri(TRIPS_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "%s", "startDate": "%s", "endDate": "%s" }
                        """.formatted(title, start, end))
                .exchange();
    }

    private MvcTestResult patch(String bearer, long id, String json) {
        return mvc.patch().uri(TRIPS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private MvcTestResult list(String bearer, String query) {
        return mvc.get().uri(TRIPS_URL + query).header(HttpHeaders.AUTHORIZATION, bearer).exchange();
    }

    private static List<String> titles(MvcTestResult result) {
        return JsonPath.read(body(result), "$.data.items[*].title");
    }

    private static long idOf(MvcTestResult result) {
        return ((Number) JsonPath.read(body(result), "$.data.id")).longValue();
    }

    // Explicit UTF-8: MockHttpServletResponse otherwise decodes as ISO-8859-1 and mangles Vietnamese
    private static String body(MvcTestResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private String bearerFor(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user).token();
    }

    private static User user(String email) {
        return User.builder()
                .email(email)
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName("Test " + email)
                .emailVerified(true)
                .build();
    }

}
