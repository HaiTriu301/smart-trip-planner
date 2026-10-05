package com.trieu.tripplanner.provider.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.exception.ProviderUnavailableException;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import com.trieu.tripplanner.support.StubHttpServer;
import com.trieu.tripplanner.support.TestProviders;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * No Spring context and no network: the provider is built by hand on a RestClient whose calls are answered by
 * MockRestServiceServer. The two sample files are real answers of Photon and Nominatim, saved on 2026-10-05:
 * a search for "chợ hàn" around Đà Nẵng, and the lookup of the first place of that search.
 */
class OsmMapProviderTest {

    private static final String PHOTON_URL = "https://photon.test";
    private static final String NOMINATIM_URL = "https://nominatim.test";
    private static final String USER_AGENT = "trip-planner-test/1.0";

    private static final ClassPathResource SEARCH_SAMPLE =
            new ClassPathResource("provider/osm/photon-search-cho-han.json");
    private static final ClassPathResource LOOKUP_SAMPLE =
            new ClassPathResource("provider/osm/nominatim-lookup-cho-han.json");

    private static final String CHO_HAN_ID = "W204885903";
    private static final Coordinate DA_NANG = new Coordinate(new BigDecimal("16.0678000"), new BigDecimal("108.2208000"));

    private MockRestServiceServer server;
    private OsmMapProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new OsmMapProvider(builder, TestProviders.osmAt(USER_AGENT, PHOTON_URL, NOMINATIM_URL));
    }

    @Test
    void placesOfThisSourceAreMarkedAsOpenStreetMap() {
        assertThat(provider.provider()).isEqualTo(PlaceProvider.OSM);
    }

    // ---- search (Photon) -----------------------------------------------------------------------------------------

    @Test
    void searchReadsThePlacesOutOfARealAnswerInTheOrderOfTheService() {
        server.expect(requestTo(startsWith(PHOTON_URL + "/api/")))
                .andRespond(withSuccess(SEARCH_SAMPLE, MediaType.APPLICATION_JSON));

        List<PlaceResult> places = provider.search("chợ hàn", 8, DA_NANG);

        assertThat(places).hasSize(8);
        // A way with a house number; coordinates in the seven decimals of the places table
        assertThat(places.get(0)).isEqualTo(new PlaceResult(PlaceProvider.OSM, CHO_HAN_ID, "Chợ Hàn",
                "119 Trần Phú, Hải Châu, Đà Nẵng", new BigDecimal("16.0683525"), new BigDecimal("108.2242830"),
                "SHOPPING"));
        // A node without a house number
        assertThat(places.get(1)).isEqualTo(new PlaceResult(PlaceProvider.OSM, "N2987529134", "Đối diện Chợ Hàn",
                "Đường Bạch Đằng, Hải Châu, Đà Nẵng", new BigDecimal("16.0680649"), new BigDecimal("108.2250231"),
                "TRANSPORT"));
        assertThat(places.get(2).category()).isEqualTo("FOOD");
        assertThat(places).extracting(PlaceResult::name).containsExactly("Chợ Hàn", "Đối diện Chợ Hàn",
                "Nhà hàng NHÀ BÊP CHÓ HÀN", "Chợ Cẩm Lệ", "Chợ Bắc Mỹ An", "Chợ Khuê Mỹ", "Chợ Non Nước", "Chan Ga Cho");
    }

    @Test
    void searchSendsTheKeywordTheLimitThePointAndSaysWhoIsCalling() {
        server.expect(requestTo(startsWith(PHOTON_URL + "/api/")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParams(Map.of("q", "chợ hàn", "limit", "8", "lat", "16.0678000", "lon", "108.2208000")))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(withSuccess(SEARCH_SAMPLE, MediaType.APPLICATION_JSON));

        provider.search("chợ hàn", 8, DA_NANG);

        server.verify();
    }

    @Test
    void searchWithoutAPointSendsNoPoint() {
        server.expect(requestTo(startsWith(PHOTON_URL + "/api/")))
                .andExpect(queryParams(Map.of("q", "chợ hàn", "limit", "5")))
                .andRespond(withSuccess(SEARCH_SAMPLE, MediaType.APPLICATION_JSON));

        // The sample holds eight places: the limit is kept even when the service sends more
        assertThat(provider.search("chợ hàn", 5, null)).hasSize(5);
        server.verify();
    }

    @Test
    void keywordWithCharactersOfAnAddressStaysOneKeyword() {
        // "&limit=50" typed by a user must not become a second limit
        String keyword = "cà phê & bánh=1+1&limit=50#x?y";
        server.expect(requestTo(startsWith(PHOTON_URL + "/api/")))
                .andExpect(queryParams(Map.of("q", keyword, "limit", "8")))
                .andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));

        assertThat(provider.search(keyword, 8, null)).isEmpty();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // no name: a street segment or a house nobody named
            "{\"properties\":{\"osm_type\":\"W\",\"osm_id\":1},\"geometry\":{\"coordinates\":[108.2,16.0]}}",
            "{\"properties\":{\"osm_type\":\"W\",\"osm_id\":1,\"name\":\"  \"},\"geometry\":{\"coordinates\":[108.2,16.0]}}",
            // no id, or an object type this application does not know
            "{\"properties\":{\"osm_type\":\"W\",\"name\":\"A\"},\"geometry\":{\"coordinates\":[108.2,16.0]}}",
            "{\"properties\":{\"osm_id\":1,\"name\":\"A\"},\"geometry\":{\"coordinates\":[108.2,16.0]}}",
            "{\"properties\":{\"osm_type\":\"X\",\"osm_id\":1,\"name\":\"A\"},\"geometry\":{\"coordinates\":[108.2,16.0]}}",
            // no position
            "{\"properties\":{\"osm_type\":\"W\",\"osm_id\":1,\"name\":\"A\"}}",
            "{\"properties\":{\"osm_type\":\"W\",\"osm_id\":1,\"name\":\"A\"},\"geometry\":{\"coordinates\":[108.2]}}",
            "{\"geometry\":{\"coordinates\":[108.2,16.0]}}",
            "null"
    })
    void placeThatCannotBeShownOrPickedIsLeftOutOfTheResults(String unusable) {
        String usable = "{\"properties\":{\"osm_type\":\"N\",\"osm_id\":7,\"name\":\"Quán B\"},"
                + "\"geometry\":{\"coordinates\":[108.21,16.05]}}";
        server.expect(requestTo(startsWith(PHOTON_URL)))
                .andRespond(withSuccess("{\"features\":[" + unusable + "," + usable + "]}", MediaType.APPLICATION_JSON));

        // No address and no category either: both are optional
        assertThat(provider.search("b", 8, null)).containsExactly(new PlaceResult(PlaceProvider.OSM, "N7", "Quán B",
                null, new BigDecimal("16.0500000"), new BigDecimal("108.2100000"), null));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "-", value = {
            "amenity  | restaurant       | FOOD",
            "amenity  | cafe             | FOOD",
            "amenity  | marketplace      | SHOPPING",
            "amenity  | bus_station      | TRANSPORT",
            "amenity  | place_of_worship | SIGHTSEEING",
            "amenity  | bank             | -",
            "tourism  | hotel            | ACCOMMODATION",
            "tourism  | guest_house      | ACCOMMODATION",
            "tourism  | museum           | SIGHTSEEING",
            "tourism  | attraction       | SIGHTSEEING",
            "tourism  | information      | -",
            "shop     | clothes          | SHOPPING",
            "historic | monument         | SIGHTSEEING",
            "natural  | beach            | SIGHTSEEING",
            "natural  | tree             | -",
            "leisure  | park             | SIGHTSEEING",
            "leisure  | pitch            | -",
            "aeroway  | aerodrome        | TRANSPORT",
            "railway  | station          | TRANSPORT",
            "railway  | rail             | -",
            "highway  | bus_stop         | TRANSPORT",
            "highway  | residential      | -",
            "building | university       | -",
            "-        | hotel            | -",
            "tourism  | -                | -"
    })
    void tagOfThePlaceSuggestsAnActivityTypeOnlyWhenItIsClear(String key, String value, String expected) {
        assertThat(OsmMapProvider.categoryOf(key, value)).isEqualTo(expected);
    }

    @Test
    void searchPutsTheCountyAndTheStateInTheAddressWhenThereIsNoCity() {
        server.expect(requestTo(startsWith(PHOTON_URL)))
                .andRespond(withSuccess("""
                        {"features":[{"properties":{"osm_type":"N","osm_id":9,"name":"Thác A","housenumber":"12",
                          "county":"Huyện B","state":"Lâm Đồng","country":"Việt Nam"},
                          "geometry":{"coordinates":[108.4,11.9]}}]}
                        """, MediaType.APPLICATION_JSON));

        // A house number without a street says nothing and is dropped
        assertThat(provider.search("thác", 8, null)).extracting(PlaceResult::address)
                .containsExactly("Huyện B, Lâm Đồng");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 429, 500, 502, 503})
    void searchErrorStatusBecomesProviderUnavailable(int status) {
        server.expect(requestTo(startsWith(PHOTON_URL)))
                .andRespond(withStatus(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"sample\"}"));

        assertUnavailable("photon", () -> provider.search("chợ hàn", 8, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not json at all", "{}", "{\"features\":null}", "{\"features\":{\"a\":1}}"})
    void searchAnswerThatCannotBeReadBecomesProviderUnavailable(String body) {
        server.expect(requestTo(startsWith(PHOTON_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertUnavailable("photon", () -> provider.search("chợ hàn", 8, null));
    }

    // ---- lookup (Nominatim) --------------------------------------------------------------------------------------

    @Test
    void lookupReadsThePlaceOutOfARealAnswer() {
        server.expect(requestTo(startsWith(NOMINATIM_URL + "/lookup")))
                .andRespond(withSuccess(LOOKUP_SAMPLE, MediaType.APPLICATION_JSON));

        assertThat(provider.lookup(CHO_HAN_ID)).contains(new PlaceResult(PlaceProvider.OSM, CHO_HAN_ID, "Chợ Hàn",
                "119 Trần Phú, Phường Hải Châu, Thành phố Đà Nẵng", new BigDecimal("16.0683525"),
                new BigDecimal("108.2242830"), "SHOPPING"));
    }

    @Test
    void lookupAsksForTheOneIdAndSaysWhoIsCalling() {
        server.expect(requestTo(startsWith(NOMINATIM_URL + "/lookup")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParams(Map.of("osm_ids", CHO_HAN_ID, "format", "jsonv2", "addressdetails", "1")))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(withSuccess(LOOKUP_SAMPLE, MediaType.APPLICATION_JSON));

        provider.lookup(CHO_HAN_ID);

        server.verify();
    }

    @Test
    void idTheServiceDoesNotKnowIsNoPlace() {
        // The real answer to an unknown id: 200 with an empty list
        server.expect(requestTo(startsWith(NOMINATIM_URL))).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(provider.lookup("N1")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "da-nang-cho-han",          // an id of the mock source
            "w204885903",               // ids are compared byte for byte
            "W204885903,N1",            // a list of ids
            "W",
            "204885903",
            "W12345678901234567890",    // longer than any number of OpenStreetMap
            "W1 ",
            ""
    })
    void idThatIsNotAnIdOfThisSourceIsNoPlaceAndTheServiceIsNotAsked(String externalId) {
        // No call is expected: any call makes the mock server fail the test
        assertThat(provider.lookup(externalId)).isEmpty();
        server.verify();
    }

    @Test
    void lookupOfAPlaceWithoutNameUsesTheFirstPartOfItsFullAddress() {
        server.expect(requestTo(startsWith(NOMINATIM_URL)))
                .andRespond(withSuccess("""
                        [{"osm_type":"node","osm_id":5,"lat":"21.0285000","lon":"105.8542000","name":"",
                          "category":"place","type":"house",
                          "display_name":"15, Phố Hàng Khay, Phường Hoàn Kiếm, Hà Nội, Việt Nam",
                          "address":{"house_number":"15","road":"Phố Hàng Khay","suburb":"Phường Hoàn Kiếm",
                                     "city":"Hà Nội","country":"Việt Nam"}}]
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.lookup("N5")).contains(new PlaceResult(PlaceProvider.OSM, "N5", "15",
                "15 Phố Hàng Khay, Phường Hoàn Kiếm, Hà Nội", new BigDecimal("21.0285000"),
                new BigDecimal("105.8542000"), null));
    }

    @Test
    void lookupAnswerWithoutAPositionIsNoPlace() {
        server.expect(requestTo(startsWith(NOMINATIM_URL)))
                .andRespond(withSuccess("[{\"name\":\"A\",\"display_name\":\"A, B\"}]", MediaType.APPLICATION_JSON));

        assertThat(provider.lookup("N5")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 429, 500, 503})
    void lookupErrorStatusBecomesProviderUnavailable(int status) {
        server.expect(requestTo(startsWith(NOMINATIM_URL)))
                .andRespond(withStatus(HttpStatus.valueOf(status)).contentType(MediaType.TEXT_HTML).body("<html>"));

        assertUnavailable("nominatim", () -> provider.lookup(CHO_HAN_ID));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not json at all", "{\"error\":\"sample\"}", "null"})
    void lookupAnswerThatCannotBeReadBecomesProviderUnavailable(String body) {
        server.expect(requestTo(startsWith(NOMINATIM_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertUnavailable("nominatim", () -> provider.lookup(CHO_HAN_ID));
    }

    // ---- both services: no answer --------------------------------------------------------------------------------

    @Test
    void serviceThatDoesNotAnswerInTimeBecomesProviderUnavailable() {
        try (StubHttpServer silent = StubHttpServer.neverAnswering()) {
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
            requestFactory.setReadTimeout(Duration.ofMillis(200));
            provider = new OsmMapProvider(RestClient.builder().requestFactory(requestFactory),
                    TestProviders.osmAt(USER_AGENT, silent.baseUrl(), silent.baseUrl()));

            assertUnavailable("photon", () -> provider.search("chợ hàn", 8, null));
            assertUnavailable("nominatim", () -> provider.lookup(CHO_HAN_ID));
        }
    }

    @Test
    void serviceThatCannotBeReachedBecomesProviderUnavailable() {
        String addressNobodyListensOn;
        try (StubHttpServer stopped = StubHttpServer.neverAnswering()) {
            addressNobodyListensOn = stopped.baseUrl();
        }
        provider = new OsmMapProvider(RestClient.builder(),
                TestProviders.osmAt(USER_AGENT, addressNobodyListensOn, addressNobodyListensOn));

        assertUnavailable("photon", () -> provider.search("chợ hàn", 8, null));
        assertUnavailable("nominatim", () -> provider.lookup(CHO_HAN_ID));
    }

    // ---- route ---------------------------------------------------------------------------------------------------

    @Test
    void routeIsEstimatedFromTheStraightLineWithoutAskingAnyService() {
        Coordinate dragonBridge = new Coordinate(new BigDecimal("16.0611000"), new BigDecimal("108.2272000"));

        // Same numbers as the mock source: both use the straight-line estimate until a routing service is connected
        assertThat(provider.route(List.of(DA_NANG, dragonBridge, DA_NANG)))
                .isEqualTo(StraightLineRoute.legs(List.of(DA_NANG, dragonBridge, DA_NANG)))
                .hasSize(2);
        server.verify();
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private static void assertUnavailable(String source, ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ProviderUnavailableException.class, ex -> {
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_UNAVAILABLE);
            assertThat(ex.getMessage()).contains(source);
        });
    }

    /** The request carries exactly these query parameters, compared after decoding: what the service will read. */
    private static RequestMatcher queryParams(Map<String, String> expected) {
        return request -> {
            Map<String, String> sent = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams()
                    .toSingleValueMap();
            sent.replaceAll((name, value) -> UriUtils.decode(value, StandardCharsets.UTF_8));
            assertThat(sent).containsExactlyInAnyOrderEntriesOf(expected);
        };
    }

}
