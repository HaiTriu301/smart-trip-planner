package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.request.CreateManualPlaceRequest;
import com.trieu.tripplanner.dto.request.SavePlaceRequest;
import com.trieu.tripplanner.dto.response.PlaceResponse;
import com.trieu.tripplanner.dto.response.PlaceResultResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.model.enums.ActivityType;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.security.JwtTokenProvider;
import com.trieu.tripplanner.service.PlaceService;
import com.trieu.tripplanner.support.TestUsers;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Web layer only: PlaceService is a mock.
 */
@WebMvcTest(PlaceController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class PlaceControllerTest {

    private static final String PLACES_URL = "/api/v1/places";
    private static final String MANUAL_URL = "/api/v1/places/manual";
    private static final String SEARCH_URL = "/api/v1/places/search";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private PlaceService placeService;

    private String bearer;

    @BeforeEach
    void signIn() {
        bearer = "Bearer " + jwtTokenProvider.generateAccessToken(TestUsers.verified(7L, "an@example.com")).token();
    }

    @Test
    void searchReturnsTheResultsWithoutAnIdOfOurs() {
        when(placeService.search("linh ung", 8, null, null)).thenReturn(List.of(new PlaceResultResponse(
                PlaceProvider.MOCK, "da-nang-chua-linh-ung", "Chùa Linh Ứng", "Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng",
                new BigDecimal("16.1001567"), new BigDecimal("108.2784112"), "SIGHTSEEING")));

        var result = mvc.get().uri(SEARCH_URL + "?q={q}", "linh ung").header(HttpHeaders.AUTHORIZATION, bearer).exchange();

        assertThat(result)
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
        // Not stored yet, so there is nothing to call it by except provider + externalId
        assertThat(result).bodyJson().doesNotHavePath("$.data[0].id");
    }

    @Test
    void limitDefaultsToEightAndCanBeChosen() {
        when(placeService.search("cho", 8, null, null)).thenReturn(List.of());
        when(placeService.search("cho", 20, null, null)).thenReturn(List.of());

        assertThat(mvc.get().uri(SEARCH_URL + "?q=cho").header(HttpHeaders.AUTHORIZATION, bearer)).hasStatusOk();
        assertThat(mvc.get().uri(SEARCH_URL + "?q=cho&limit=20").header(HttpHeaders.AUTHORIZATION, bearer)).hasStatusOk();

        verify(placeService).search("cho", 8, null, null);
        verify(placeService).search("cho", 20, null, null);
    }

    @Test
    void noMatchIsAnEmptyListNotAnError() {
        when(placeService.search("khong co noi nay", 8, null, null)).thenReturn(List.of());

        assertThat(mvc.get().uri(SEARCH_URL + "?q={q}", "khong co noi nay").header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true, "data": [] }
                        """);
    }

    @Test
    void searchWithoutLoginIsRejected() {
        assertThat(mvc.get().uri(SEARCH_URL + "?q=cho"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(placeService);
    }

    @Test
    void missingKeywordIsAValidationErrorOnQ() {
        assertThat(mvc.get().uri(SEARCH_URL).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "q", "message": "Thiếu tham số bắt buộc" } ]
                        }
                        """);
        verifyNoInteractions(placeService);
    }

    @ParameterizedTest
    // One letter, one letter between spaces, only spaces, nothing
    @ValueSource(strings = {"a", "  a  ", "   ", ""})
    void keywordShorterThanTwoCharactersIsRejected(String keyword) {
        assertThat(mvc.get().uri(SEARCH_URL + "?q={q}", keyword).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "q", "message": "Từ khoá tìm địa điểm cần ít nhất 2 ký tự" } ]
                        }
                        """);
        verifyNoInteractions(placeService);
    }

    @Test
    void twoCharactersAreEnoughAndOneHundredIsTheMost() {
        String longest = "a".repeat(100);
        when(placeService.search("ga", 8, null, null)).thenReturn(List.of());
        when(placeService.search(longest, 8, null, null)).thenReturn(List.of());

        assertThat(mvc.get().uri(SEARCH_URL + "?q=ga").header(HttpHeaders.AUTHORIZATION, bearer)).hasStatusOk();
        assertThat(mvc.get().uri(SEARCH_URL + "?q={q}", longest).header(HttpHeaders.AUTHORIZATION, bearer)).hasStatusOk();

        assertThat(mvc.get().uri(SEARCH_URL + "?q={q}", "a".repeat(101)).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "q", "message": "Từ khoá tìm địa điểm không được vượt quá 100 ký tự" } ]
                        }
                        """);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "21", "-1"})
    void limitOutsideOneToTwentyIsRejected(String limit) {
        assertThat(mvc.get().uri(SEARCH_URL + "?q=cho&limit=" + limit).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "limit", "message": "Số kết quả phải nằm trong khoảng 1 đến 20" } ]
                        }
                        """);
        verifyNoInteractions(placeService);
    }

    @Test
    void referencePointIsPassedOnWhenSent() {
        BigDecimal lat = new BigDecimal("16.0544");
        BigDecimal lng = new BigDecimal("108.2022");
        when(placeService.search("cho", 8, lat, lng)).thenReturn(List.of());

        assertThat(mvc.get().uri(SEARCH_URL + "?q=cho&lat=16.0544&lng=108.2022").header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatusOk();

        verify(placeService).search("cho", 8, lat, lng);
    }

    @Test
    void halfACoordinateIsReportedOnTheMissingNumber() {
        when(placeService.search("cho", 8, new BigDecimal("16.0544"), null)).thenThrow(BusinessRuleException.invalidField(
                "lng", "error.place.coordinates-incomplete", "Only one of lat/lng was sent to the place search"));

        assertThat(mvc.get().uri(SEARCH_URL + "?q=cho&lat=16.0544").header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "lng", "message": "Cần gửi đủ cả vĩ độ và kinh độ, hoặc bỏ cả hai" } ]
                        }
                        """);
    }

    @ParameterizedTest
    @ValueSource(strings = {"lat=90.0000001&lng=108", "lat=-91&lng=108"})
    void latitudeOutsideMinus90To90IsRejected(String coordinate) {
        assertThat(mvc.get().uri(SEARCH_URL + "?q=cho&" + coordinate).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "lat", "message": "Vĩ độ phải nằm trong khoảng -90 đến 90" } ]
                        }
                        """);
        verifyNoInteractions(placeService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"lat=16&lng=180.0000001", "lat=16&lng=-181"})
    void longitudeOutsideMinus180To180IsRejected(String coordinate) {
        assertThat(mvc.get().uri(SEARCH_URL + "?q=cho&" + coordinate).header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "lng", "message": "Kinh độ phải nằm trong khoảng -180 đến 180" } ]
                        }
                        """);
        verifyNoInteractions(placeService);
    }

    @Test
    void edgesOfTheMapAreAccepted() {
        when(placeService.search("cho", 8, new BigDecimal("90"), new BigDecimal("-180"))).thenReturn(List.of());

        assertThat(mvc.get().uri(SEARCH_URL + "?q=cho&lat=90&lng=-180").header(HttpHeaders.AUTHORIZATION, bearer)).hasStatusOk();
    }

    @Test
    void coordinateThatIsNotANumberIsAValidationError() {
        assertThat(mvc.get().uri(SEARCH_URL + "?q=cho&lat=bac&lng=108").header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(placeService);
    }

    // ---------- POST /places ----------

    @Test
    void pickingAResultReturnsThePlaceWithItsId() {
        when(placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "da-nang-chua-linh-ung")))
                .thenReturn(new PlaceResponse(31L, PlaceProvider.MOCK, "Chùa Linh Ứng",
                        "Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng", new BigDecimal("16.1001567"),
                        new BigDecimal("108.2784112"), "SIGHTSEEING"));

        assertThat(save("""
                {"provider": "MOCK", "externalId": "da-nang-chua-linh-ung"}
                """))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": true,
                          "data": {
                            "id": 31,
                            "provider": "MOCK",
                            "name": "Chùa Linh Ứng",
                            "address": "Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng",
                            "lat": 16.1001567,
                            "lng": 108.2784112,
                            "category": "SIGHTSEEING"
                          }
                        }
                        """);
    }

    @Test
    void nameAndCoordinatesSentByTheClientNeverReachTheService() {
        when(placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "da-nang-chua-linh-ung")))
                .thenReturn(new PlaceResponse(31L, PlaceProvider.MOCK, "Chùa Linh Ứng", null, new BigDecimal("16.1"),
                        new BigDecimal("108.2"), null));

        // Extra fields are dropped: the request type has no place to keep them
        assertThat(save("""
                {"provider": "MOCK", "externalId": "da-nang-chua-linh-ung", "name": "Giữa biển", "lat": 10, "lng": 115}
                """)).hasStatusOk();

        verify(placeService).getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "da-nang-chua-linh-ung"));
    }

    @Test
    void pickingWithoutLoginIsRejected() {
        assertThat(mvc.post().uri(PLACES_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"provider": "MOCK", "externalId": "da-nang-chua-linh-ung"}
                """))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(placeService);
    }

    @Test
    void pickingAPlaceTheSourceDoesNotKnowIsNotFound() {
        when(placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "bia-ra")))
                .thenThrow(new ResourceNotFoundException("Place", "MOCK/bia-ra"));

        assertThat(save("""
                {"provider": "MOCK", "externalId": "bia-ra"}
                """))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "RESOURCE_NOT_FOUND", "message": "Không tìm thấy dữ liệu yêu cầu" }
                        """);
    }

    @Test
    void missingProviderAndBlankExternalIdAreBothReported() {
        assertThat(save("""
                {"externalId": "   "}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [
                            { "field": "externalId", "message": "Thiếu mã của địa điểm" },
                            { "field": "provider", "message": "Thiếu nguồn của địa điểm" }
                          ]
                        }
                        """);
        verifyNoInteractions(placeService);
    }

    @Test
    void externalIdLongerThanTheColumnIsRejected() {
        assertThat(save("""
                {"provider": "MOCK", "externalId": "%s"}
                """.formatted("a".repeat(129))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "details": [ { "field": "externalId", "message": "Mã của địa điểm không được vượt quá 128 ký tự" } ] }
                        """);
        verifyNoInteractions(placeService);
    }

    @Test
    void sourceTheAppDoesNotHaveIsAValidationError() {
        assertThat(save("""
                {"provider": "GOOGLE", "externalId": "abc"}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(placeService);
    }

    // ---------- POST /places/manual ----------

    @Test
    void addingAPlaceByHandReturns201AndUsesTheSignedInUserAsCreator() {
        CreateManualPlaceRequest request = new CreateManualPlaceRequest("Nhà bà ngoại", "12 Lê Lợi, Đà Nẵng",
                new BigDecimal("16.0471234"), new BigDecimal("108.2068765"), ActivityType.ACCOMMODATION);
        when(placeService.createManual(7L, request)).thenReturn(new PlaceResponse(40L, PlaceProvider.MANUAL,
                "Nhà bà ngoại", "12 Lê Lợi, Đà Nẵng", new BigDecimal("16.0471234"), new BigDecimal("108.2068765"),
                "ACCOMMODATION"));

        // createdBy in the body is ignored: the creator is the user inside the token (id 7)
        assertThat(manual("""
                {"name": "Nhà bà ngoại", "address": "12 Lê Lợi, Đà Nẵng", "lat": 16.0471234, "lng": 108.2068765,
                 "category": "ACCOMMODATION", "createdBy": 99}
                """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": true,
                          "data": { "id": 40, "provider": "MANUAL", "name": "Nhà bà ngoại", "lat": 16.0471234,
                                    "lng": 108.2068765, "category": "ACCOMMODATION" }
                        }
                        """);
        verify(placeService).createManual(7L, request);
    }

    @Test
    void onlyNameAndCoordinatesAreRequired() {
        CreateManualPlaceRequest request = new CreateManualPlaceRequest("Điểm hẹn", null, new BigDecimal("16"),
                new BigDecimal("108"), null);
        when(placeService.createManual(7L, request)).thenReturn(new PlaceResponse(41L, PlaceProvider.MANUAL, "Điểm hẹn",
                null, new BigDecimal("16"), new BigDecimal("108"), null));

        assertThat(manual("""
                {"name": "Điểm hẹn", "lat": 16, "lng": 108}
                """)).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void addingAPlaceWithoutLoginIsRejected() {
        assertThat(mvc.post().uri(MANUAL_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"name": "Điểm hẹn", "lat": 16, "lng": 108}
                """)).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(placeService);
    }

    @Test
    void missingNameAndCoordinatesAreEachReported() {
        assertThat(manual("""
                {"name": "  "}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [
                            { "field": "lat", "message": "Thiếu vĩ độ của địa điểm" },
                            { "field": "lng", "message": "Thiếu kinh độ của địa điểm" },
                            { "field": "name", "message": "Tên địa điểm không được để trống" }
                          ]
                        }
                        """);
        verifyNoInteractions(placeService);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "90.0000001 | 108          | lat | Vĩ độ phải nằm trong khoảng -90 đến 90",
            "-91        | 108          | lat | Vĩ độ phải nằm trong khoảng -90 đến 90",
            "16         | 180.0000001  | lng | Kinh độ phải nằm trong khoảng -180 đến 180",
            "16         | -181         | lng | Kinh độ phải nằm trong khoảng -180 đến 180",
            "16.12345678 | 108         | lat | Toạ độ có tối đa 7 chữ số thập phân"})
    void coordinatesOutsideTheGlobeOrTooPreciseAreRejected(String lat, String lng, String field, String message) {
        assertThat(manual("""
                {"name": "Điểm hẹn", "lat": %s, "lng": %s}
                """.formatted(lat, lng)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        { "errorCode": "VALIDATION_ERROR", "details": [ { "field": "%s", "message": "%s" } ] }
                        """.formatted(field, message));
        verifyNoInteractions(placeService);
    }

    @Test
    void nameAndAddressLongerThanTheirColumnsAreRejected() {
        assertThat(manual("""
                {"name": "%s", "address": "%s", "lat": 16, "lng": 108}
                """.formatted("a".repeat(201), "b".repeat(501))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "details": [
                            { "field": "address", "message": "Địa chỉ không được vượt quá 500 ký tự" },
                            { "field": "name", "message": "Tên địa điểm không được vượt quá 200 ký tự" }
                          ]
                        }
                        """);
    }

    @Test
    void categoryThatIsNotAnActivityTypeIsAValidationError() {
        assertThat(manual("""
                {"name": "Điểm hẹn", "lat": 16, "lng": 108, "category": "CASINO"}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
        verifyNoInteractions(placeService);
    }

    private MvcTestResult manual(String body) {
        return mvc.post().uri(MANUAL_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult save(String body) {
        return mvc.post().uri(PLACES_URL).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    @Test
    void limitThatIsNotANumberIsAValidationError() {
        assertThat(mvc.get().uri(SEARCH_URL + "?q=cho&limit=nhieu").header(HttpHeaders.AUTHORIZATION, bearer))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errorCode").isEqualTo("VALIDATION_ERROR");
    }

}
