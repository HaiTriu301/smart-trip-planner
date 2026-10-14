package com.trieu.tripplanner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.config.SecurityConfig;
import com.trieu.tripplanner.dto.response.PublicTripResponse;
import com.trieu.tripplanner.dto.response.PublicTripResponse.PublicActivity;
import com.trieu.tripplanner.dto.response.PublicTripResponse.PublicDay;
import com.trieu.tripplanner.dto.response.PublicTripResponse.PublicPlace;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.model.enums.ActivityType;
import com.trieu.tripplanner.service.PublicTripService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Web layer only: PublicTripService is a mock. The real SecurityConfig is imported so the page is proven to open
 * without an account, which is the whole point of it.
 */
@WebMvcTest(PublicTripController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class PublicTripControllerTest {

    private static final String TOKEN = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
    private static final String PAGE_URL = "/api/v1/public/trips/" + TOKEN;

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private PublicTripService publicTripService;

    @Test
    void opensWithoutAnAccountAndShowsTheItinerary() {
        when(publicTripService.open(TOKEN)).thenReturn(page());

        // No Authorization header at all
        assertThat(mvc.get().uri(PAGE_URL))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        { "success": true,
                          "data": {
                            "title": "Đà Lạt 3 ngày", "description": "Đi cùng nhóm bạn đại học",
                            "destinationName": "Đà Lạt", "destinationLat": 11.9404190, "destinationLng": 108.4583070,
                            "startDate": "2026-11-01", "endDate": "2026-11-02",
                            "days": [
                              { "dayIndex": 1, "date": "2026-11-01", "title": "Trung tâm",
                                "activities": [
                                  { "title": "Ăn sáng", "type": "FOOD", "startTime": "07:30", "endTime": "08:15",
                                    "note": "Bánh căn",
                                    "place": { "name": "Chợ Đà Lạt", "address": "Nguyễn Thị Minh Khai, Phường 1",
                                               "lat": 11.9429330, "lng": 108.4371380 } } ] },
                              { "dayIndex": 2, "date": "2026-11-02", "title": null, "activities": [] } ] } }
                        """);
    }

    @Test
    void thePageCarriesNothingBeyondWhatAGuestMaySee() {
        when(publicTripService.open(TOKEN)).thenReturn(page());

        MvcTestResult result = mvc.get().uri(PAGE_URL).exchange();

        // No id of any row, no owner, no member, no cost, no booking link, no note of a day, no status
        assertThat(result).bodyJson().extractingPath("$.data").asMap().containsOnlyKeys(
                "title", "description", "destinationName", "destinationLat", "destinationLng", "startDate", "endDate",
                "days");
        assertThat(result).bodyJson().extractingPath("$.data.days[0]").asMap().containsOnlyKeys(
                "dayIndex", "date", "title", "activities");
        assertThat(result).bodyJson().extractingPath("$.data.days[0].activities[0]").asMap().containsOnlyKeys(
                "title", "type", "startTime", "endTime", "note", "place");
        assertThat(result).bodyJson().extractingPath("$.data.days[0].activities[0].place").asMap().containsOnlyKeys(
                "name", "address", "lat", "lng");
    }

    @Test
    void aBrokenOrStaleBearerTokenDoesNotStopTheGuest() {
        // A signed-in owner whose access token ran out opens their own link: the page must not answer 401
        when(publicTripService.open(TOKEN)).thenReturn(page());

        assertThat(mvc.get().uri(PAGE_URL).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .hasStatusOk();
    }

    @Test
    void aRefusedLinkIsThe404OfAnyMissingResource() {
        when(publicTripService.open(TOKEN)).thenThrow(new ResourceNotFoundException("ShareLink", "41 (revoked)"));

        // Nothing in the body says whether the token never existed, was revoked or expired
        assertThat(mvc.get().uri(PAGE_URL))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        { "success": false, "errorCode": "RESOURCE_NOT_FOUND",
                          "message": "Không tìm thấy dữ liệu yêu cầu" }
                        """);
    }

    @Test
    void theTokenOfARefusedPageNeverReachesTheLog(CapturedOutput output) {
        when(publicTripService.open(TOKEN)).thenThrow(new ResourceNotFoundException("ShareLink", "41 (expired)"));

        assertThat(mvc.get().uri(PAGE_URL)).hasStatus(HttpStatus.NOT_FOUND);

        // The 404 is logged with the path, and the path holds the token (CLAUDE.md rule 17)
        assertThat(output).contains("RESOURCE_NOT_FOUND on /api/v1/public/trips/***");
        assertThat(output).doesNotContain(TOKEN);
    }

    @Test
    void guestsCanOnlyRead() {
        // /api/v1/public/** is open to every method in SecurityConfig: only the GET mapping exists behind it
        assertThat(mvc.post().uri(PAGE_URL)).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(mvc.delete().uri(PAGE_URL)).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
        verifyNoInteractions(publicTripService);
    }

    /** The one place of this class that builds the response, so a new field is added here only. */
    private static PublicTripResponse page() {
        PublicActivity breakfast = new PublicActivity("Ăn sáng", ActivityType.FOOD, LocalTime.of(7, 30),
                LocalTime.of(8, 15), "Bánh căn", new PublicPlace("Chợ Đà Lạt", "Nguyễn Thị Minh Khai, Phường 1",
                new BigDecimal("11.9429330"), new BigDecimal("108.4371380")));
        return new PublicTripResponse("Đà Lạt 3 ngày", "Đi cùng nhóm bạn đại học", "Đà Lạt",
                new BigDecimal("11.9404190"), new BigDecimal("108.4583070"), LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 2), List.of(
                        new PublicDay(1, LocalDate.of(2026, 11, 1), "Trung tâm", List.of(breakfast)),
                        new PublicDay(2, LocalDate.of(2026, 11, 2), null, List.of())));
    }

}
