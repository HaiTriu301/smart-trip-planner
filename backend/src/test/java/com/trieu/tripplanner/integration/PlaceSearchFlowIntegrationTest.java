package com.trieu.tripplanner.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.dto.request.SavePlaceRequest;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.MockMapProvider;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.service.PlaceService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Place search, picking a result and adding a place by hand, through every layer of the real application:
 * security filter → controller → service → the map source Spring actually wires in (the bundled data) → MySQL →
 * JSON. Nothing is mocked. Attaching a place to an activity is in ActivityPlaceFlowIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PlaceSearchFlowIntegrationTest {

    private static final String PLACES_URL = "/api/v1/places";
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

    @Autowired
    private PlaceService placeService;

    private Long userId;
    private String bearer;

    @BeforeEach
    void signIn() {
        User user = userRepository.save(User.builder()
                .email("traveller@example.com")
                .passwordHash("$2a$12$placeholder-bcrypt-hash-not-real")
                .fullName("Người đi chơi")
                .emailVerified(true)
                .build());
        userId = user.getId();
        bearer = "Bearer " + jwtTokenProvider.generateAccessToken(user).token();
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM places");
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

    // ---------- picking a result (POST /places) ----------

    @Test
    void pickingASearchResultStoresItOnceAndAlwaysAnswersWithTheSameId() {
        // What the client holds after a search: the two values that name the place
        String found = body(search("?q={q}", "bun cha ca"));
        String provider = JsonPath.read(found, "$.data[0].provider");
        String externalId = JsonPath.read(found, "$.data[0].externalId");

        MvcTestResult first = pick(provider, externalId);
        MvcTestResult second = pick(provider, externalId);

        assertThat(first)
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": true,
                          "data": {
                            "provider": "MOCK",
                            "name": "Bún chả cá 109",
                            "address": "Đường Nguyễn Chí Thanh, Phường Hải Châu, Đà Nẵng",
                            "lat": 16.0743887,
                            "lng": 108.2207958,
                            "category": "FOOD"
                          }
                        }
                        """);
        Number firstId = JsonPath.read(body(first), "$.data.id");
        Number secondId = JsonPath.read(body(second), "$.data.id");
        assertThat(firstId.longValue()).isPositive().isEqualTo(secondId.longValue());
        assertThat(storedPlaces()).isEqualTo(1);
    }

    @Test
    void storedCopyComesFromTheSourceWhateverTheClientSends() {
        // A client bypassing the screen tries to plant its own name and coordinates for a real place
        MvcTestResult forged = mvc.post().uri(PLACES_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"provider": "MOCK", "externalId": "da-nang-chua-linh-ung", "name": "Giữa biển", "lat": 10, "lng": 115}
                        """)
                .exchange();

        assertThat(forged).hasStatusOk();
        assertThat(jdbcTemplate.queryForMap("SELECT name, lat, lng FROM places WHERE external_id = 'da-nang-chua-linh-ung'"))
                .containsEntry("name", "Chùa Linh Ứng")
                .containsEntry("lat", new BigDecimal("16.1001567"))
                .containsEntry("lng", new BigDecimal("108.2784112"));
    }

    @Test
    void pickingAPlaceTheSourceDoesNotKnowIsNotFoundAndStoresNothing() {
        assertThat(pick("MOCK", "da-nang-khong-co"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(storedPlaces()).isZero();
    }

    @Test
    void manyUsersPickingTheSamePlaceAtTheSameMomentShareOneRow() throws Exception {
        int users = 8;
        SavePlaceRequest request = new SavePlaceRequest(PlaceProvider.MOCK, "da-nang-cho-han");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(users);
        try {
            List<Future<Long>> answers = new ArrayList<>();
            for (int i = 0; i < users; i++) {
                answers.add(pool.submit(() -> {
                    start.await();
                    return placeService.getOrCreate(request).id();
                }));
            }
            start.countDown();

            Set<Long> ids = new HashSet<>();
            for (Future<Long> answer : answers) {
                ids.add(answer.get(20, TimeUnit.SECONDS));
            }

            // Whoever wins the INSERT, nobody gets an error and nobody gets a different id
            assertThat(ids).hasSize(1);
            assertThat(storedPlaces()).isEqualTo(1);
        }
        finally {
            pool.shutdownNow();
        }
    }

    @Test
    void pickingFromASourceThatIsNotInUseIsRefusedAndStoresNothing() {
        // MANUAL with an id the bundled data does know: before the check existed this stored a row
        assertThat(pick("MANUAL", "da-nang-cho-han"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "provider", "message": "Nguồn địa điểm này hiện không dùng được" } ]
                        }
                        """);
        assertThat(storedPlaces()).isZero();
    }

    @Test
    void pickingWithoutATokenIsRejected() {
        assertThat(mvc.post().uri(PLACES_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"provider": "MOCK", "externalId": "da-nang-cho-han"}
                """)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(storedPlaces()).isZero();
    }

    // ---------- adding a place by hand (POST /places/manual) ----------

    @Test
    void placeAddedByHandBelongsToTheSignedInUserAndIsNeverMergedWithAnother() {
        String home = """
                {"name": "  Nhà bà ngoại ", "address": "12 Lê Lợi, Đà Nẵng", "lat": 16.0471234, "lng": 108.2068765,
                 "category": "ACCOMMODATION", "createdBy": 999999}
                """;

        MvcTestResult first = addByHand(home);
        MvcTestResult second = addByHand(home);

        assertThat(first)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": true,
                          "data": { "provider": "MANUAL", "name": "Nhà bà ngoại", "address": "12 Lê Lợi, Đà Nẵng",
                                    "lat": 16.0471234, "lng": 108.2068765, "category": "ACCOMMODATION" }
                        }
                        """);
        // The same name twice is two places: two users, or one user, may mean two different houses
        Number firstId = JsonPath.read(body(first), "$.data.id");
        Number secondId = JsonPath.read(body(second), "$.data.id");
        assertThat(firstId.longValue()).isNotEqualTo(secondId.longValue());
        assertThat(storedPlaces()).isEqualTo(2);
        // The creator is the user of the token, whatever the body claims; no id of a source
        assertThat(jdbcTemplate.queryForMap("SELECT external_id, created_by FROM places WHERE id = ?", firstId))
                .containsEntry("external_id", null)
                .containsEntry("created_by", userId);
    }

    @Test
    void placeAddedByHandWithoutCoordinatesIsRefusedAndStoresNothing() {
        assertThat(addByHand("""
                {"name": "Điểm hẹn", "lat": 91}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [
                            { "field": "lat", "message": "Vĩ độ phải nằm trong khoảng -90 đến 90" },
                            { "field": "lng", "message": "Thiếu kinh độ của địa điểm" }
                          ]
                        }
                        """);
        assertThat(storedPlaces()).isZero();
    }

    // ---------- helpers ----------

    private MvcTestResult addByHand(String json) {
        return mvc.post().uri(PLACES_URL + "/manual").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private MvcTestResult pick(String provider, String externalId) {
        return mvc.post().uri(PLACES_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"provider": "%s", "externalId": "%s"}
                        """.formatted(provider, externalId))
                .exchange();
    }

    private int storedPlaces() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM places", Integer.class);
        return count == null ? 0 : count;
    }

    // Explicit UTF-8: MockHttpServletResponse otherwise decodes as ISO-8859-1 and mangles Vietnamese
    private static String body(MvcTestResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private MvcTestResult search(String query, Object... uriVariables) {
        return mvc.get().uri(SEARCH_URL + query, uriVariables).header(HttpHeaders.AUTHORIZATION, bearer).exchange();
    }

    private static List<String> names(MvcTestResult result) {
        assertThat(result).hasStatusOk();
        return JsonPath.read(body(result), "$.data[*].name");
    }

}
