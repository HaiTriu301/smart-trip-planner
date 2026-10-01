package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.MockMapProvider;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Place search through every layer of the real application: security filter → controller → service → the map
 * source Spring actually wires in (the bundled data) → JSON. Nothing is mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PlaceSearchFlowIntegrationTest {

    private static final String SEARCH_URL = "/api/v1/places/search";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ApplicationContext applicationContext;

    private String bearer;

    @BeforeEach
    void signIn() {
        User user = userRepository.save(User.builder()
                .email("traveller@example.com")
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName("Người đi chơi")
                .emailVerified(true)
                .build());
        bearer = "Bearer " + jwtTokenProvider.generateAccessToken(user).token();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void theBundledDataIsTheOnlyMapSourceWhenNothingElseIsConfigured() {
        // No API key, no network: the default configuration must give a working search (CLAUDE.md rule 20)
        assertThat(applicationContext.getBeansOfType(MapProvider.class).values())
                .singleElement()
                .isInstanceOf(MockMapProvider.class);
    }

    @Test
    void signedInUserFindsAPlaceWhetherTheKeywordHasAccentsOrNot() {
        MvcTestResult plain = search("?q={q}", "linh ung");

        assertThat(plain)
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": true,
                          "data": [
                            {
                              "provider": "MOCK",
                              "externalId": "da-nang-chua-linh-ung",
                              "name": "Chùa Linh Ứng",
                              "address": "Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng",
                              "lat": 16.1001567,
                              "lng": 108.2784112,
                              "category": "SIGHTSEEING"
                            }
                          ]
                        }
                        """);
        assertThat(plain).bodyJson().doesNotHavePath("$.data[0].id");
        // The accents survive the URL and the servlet decoding, and lead to the same place
        assertThat(names(search("?q={q}", "Chùa Linh Ứng"))).containsExactly("Chùa Linh Ứng");
    }

    @Test
    void referencePointPutsTheCityBeingPlannedFirstWithoutDroppingAnyResult() {
        List<String> noPreference = names(search("?q=cho&limit=20"));
        List<String> fromHoChiMinh = names(search("?q=cho&limit=20&lat=10.7769&lng=106.7009"));
        List<String> fromHaNoi = names(search("?q=cho&limit=20&lat=21.0285&lng=105.8542"));

        assertThat(noPreference).startsWith("Chợ Hàn", "Chợ Cồn");
        assertThat(fromHoChiMinh).startsWith("Chợ Bến Thành", "Chợ Bình Tây");
        assertThat(fromHaNoi).startsWith("Chợ Đồng Xuân");
        assertThat(fromHoChiMinh).containsExactlyInAnyOrderElementsOf(noPreference);
        assertThat(fromHaNoi).containsExactlyInAnyOrderElementsOf(noPreference);
    }

    @Test
    void limitCutsTheListAndNoMatchIsAnEmptyList() {
        assertThat(names(search("?q=cho&limit=2"))).containsExactly("Chợ Hàn", "Chợ Cồn");
        assertThat(search("?q={q}", "khong co noi nay"))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true, "data": [] }
                        """);
    }

    @Test
    void mistakesOfTheCallerComeBackAsValidationErrorsInVietnamese() {
        assertThat(search(""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": false,
                          "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "q", "message": "Thiếu tham số bắt buộc" } ],
                          "path": "/api/v1/places/search"
                        }
                        """);
        assertThat(search("?q=a"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "details": [ { "field": "q", "message": "Từ khoá tìm địa điểm cần ít nhất 2 ký tự" } ] }
                        """);
        // The pair rule lives in the service: only the full application shows it reaching the client
        assertThat(search("?q=cho&lat=16.05"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "lng", "message": "Cần gửi đủ cả vĩ độ và kinh độ, hoặc bỏ cả hai" } ]
                        }
                        """);
    }

    @Test
    void searchWithoutATokenIsRejected() {
        assertThat(mvc.get().uri(SEARCH_URL + "?q=cho"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
    }

    // ---------- helpers ----------

    private MvcTestResult search(String query, Object... uriVariables) {
        return mvc.get().uri(SEARCH_URL + query, uriVariables).header(HttpHeaders.AUTHORIZATION, bearer).exchange();
    }

    private static List<String> names(MvcTestResult result) {
        assertThat(result).hasStatusOk();
        // Explicit UTF-8: MockHttpServletResponse otherwise decodes as ISO-8859-1 and mangles Vietnamese
        return JsonPath.read(new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8),
                "$.data[*].name");
    }

}
