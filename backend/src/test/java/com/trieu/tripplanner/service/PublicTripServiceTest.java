package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.dto.response.PublicTripResponse;
import com.trieu.tripplanner.dto.response.PublicTripResponse.PublicActivity;
import com.trieu.tripplanner.dto.response.PublicTripResponse.PublicDay;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.PublicTripMapper;
import com.trieu.tripplanner.model.Activity;
import com.trieu.tripplanner.model.Place;
import com.trieu.tripplanner.model.ShareLink;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.model.User;
import com.trieu.tripplanner.model.enums.ActivityType;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.repository.ActivityRepository;
import com.trieu.tripplanner.repository.ShareLinkRepository;
import com.trieu.tripplanner.repository.TripDayRepository;
import com.trieu.tripplanner.support.TestUsers;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test of the public page: repositories are mocks, the mapper is the generated one, the clock stands still.
 * That the three queries really find the rows on MySQL is proven by the flow test of Mốc 9.
 */
@ExtendWith(MockitoExtension.class)
class PublicTripServiceTest {

    private static final long TRIP_ID = 5L;
    private static final long LINK_ID = 41L;
    private static final String TOKEN = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");

    @Mock
    private ShareLinkRepository shareLinkRepository;

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private ActivityRepository activityRepository;

    private PublicTripService publicTripService;
    private User owner;
    private Trip trip;

    @BeforeEach
    void setUp() {
        publicTripService = new PublicTripService(shareLinkRepository, tripDayRepository, activityRepository,
                Mappers.getMapper(PublicTripMapper.class), Clock.fixed(NOW, ZoneOffset.UTC));
        owner = TestUsers.verified(7L, "owner@example.com");
        trip = Trip.builder()
                .owner(owner)
                .title("Đà Lạt 3 ngày")
                .slug("da-lat-3-ngay-x7k2qp")
                .description("Đi cùng nhóm bạn đại học")
                .destinationName("Đà Lạt")
                .destinationLat(new BigDecimal("11.9404190"))
                .destinationLng(new BigDecimal("108.4583070"))
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 2))
                .budgetAmount(new BigDecimal("5000000.00"))
                .build();
        ReflectionTestUtils.setField(trip, "id", TRIP_ID);
    }

    @Test
    void opensTheTripOfALiveLinkWithEveryDayAndItsActivitiesInOrder() {
        linkOfTheToken(link(null));
        TripDay first = day(11L, 1, LocalDate.of(2026, 11, 1), "Trung tâm");
        TripDay second = day(12L, 2, LocalDate.of(2026, 11, 2), null);
        when(tripDayRepository.findByTripIdOrderByDate(TRIP_ID)).thenReturn(List.of(first, second));
        Place market = Place.builder().provider(PlaceProvider.MOCK).externalId("dl-001").name("Chợ Đà Lạt")
                .address("Nguyễn Thị Minh Khai, Phường 1").lat(new BigDecimal("11.9429330"))
                .lng(new BigDecimal("108.4371380")).category("FOOD").build();
        Activity breakfast = activity(first, "Ăn sáng", ActivityType.FOOD, LocalTime.of(7, 30), LocalTime.of(8, 15),
                "Bánh căn", market);
        Activity walk = activity(first, "Dạo hồ", ActivityType.SIGHTSEEING, null, null, null, null);
        when(activityRepository.findByTripIdInDisplayOrder(TRIP_ID)).thenReturn(List.of(breakfast, walk));

        PublicTripResponse page = publicTripService.open(TOKEN);

        assertThat(page.title()).isEqualTo("Đà Lạt 3 ngày");
        assertThat(page.description()).isEqualTo("Đi cùng nhóm bạn đại học");
        assertThat(page.destinationName()).isEqualTo("Đà Lạt");
        assertThat(page.destinationLat()).isEqualByComparingTo("11.9404190");
        assertThat(page.destinationLng()).isEqualByComparingTo("108.4583070");
        assertThat(page.startDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(page.endDate()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(page.days()).extracting(PublicDay::dayIndex, PublicDay::date, PublicDay::title).containsExactly(
                tuple(1, LocalDate.of(2026, 11, 1), "Trung tâm"),
                tuple(2, LocalDate.of(2026, 11, 2), null));
        // The order of the query is kept inside the day; a day without activities still shows
        assertThat(page.days().get(0).activities()).extracting(PublicActivity::title)
                .containsExactly("Ăn sáng", "Dạo hồ");
        assertThat(page.days().get(1).activities()).isEmpty();

        PublicActivity shown = page.days().get(0).activities().get(0);
        assertThat(shown.type()).isEqualTo(ActivityType.FOOD);
        assertThat(shown.startTime()).isEqualTo(LocalTime.of(7, 30));
        assertThat(shown.endTime()).isEqualTo(LocalTime.of(8, 15));
        assertThat(shown.note()).isEqualTo("Bánh căn");
        assertThat(shown.place().name()).isEqualTo("Chợ Đà Lạt");
        assertThat(shown.place().address()).isEqualTo("Nguyễn Thị Minh Khai, Phường 1");
        assertThat(shown.place().lat()).isEqualByComparingTo("11.9429330");
        assertThat(shown.place().lng()).isEqualByComparingTo("108.4371380");
        assertThat(page.days().get(0).activities().get(1).place()).isNull();
    }

    @Test
    void aLinkWhoseExpiryIsStillAheadOpensTheTrip() {
        linkOfTheToken(link(NOW.plus(1, ChronoUnit.MICROS)));
        when(tripDayRepository.findByTripIdOrderByDate(TRIP_ID)).thenReturn(List.of());
        when(activityRepository.findByTripIdInDisplayOrder(TRIP_ID)).thenReturn(List.of());

        assertThat(publicTripService.open(TOKEN).title()).isEqualTo("Đà Lạt 3 ngày");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {
            "",
            "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p",      // 31 characters
            "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6x",    // 33 characters
            "A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6",     // upper case: the _ci column would find the real link
            "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p-",
            "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5 6",
            "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5pđ",
            "' or '1'='1' -- a1b2c3d4e5f6g7h8"})
    void aMalformedTokenIs404WithoutAskingTheDatabase(String token) {
        assertThatThrownBy(() -> publicTripService.open(token))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(shareLinkRepository, tripDayRepository, activityRepository);
    }

    @Test
    void anUnknownTokenIs404AndTheMessageDoesNotCarryIt() {
        when(shareLinkRepository.findWithTripByToken(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicTripService.open(TOKEN))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageNotContaining(TOKEN);
        verifyNoInteractions(tripDayRepository, activityRepository);
    }

    @Test
    void aRevokedLinkIs404AndNothingOfTheTripIsRead() {
        ShareLink revoked = link(null);
        revoked.revoke(NOW.minus(1, ChronoUnit.HOURS));
        linkOfTheToken(revoked);

        assertThatThrownBy(() -> publicTripService.open(TOKEN))
                .isInstanceOf(ResourceNotFoundException.class)
                // The log learns which link and why, never the token
                .hasMessageContaining("41 (revoked")
                .hasMessageNotContaining(TOKEN);
        verifyNoInteractions(tripDayRepository, activityRepository);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("expiriesThatAreNoLongerAhead")
    void anExpiredLinkIs404AndNothingOfTheTripIsRead(String label, Instant expiresAt) {
        linkOfTheToken(link(expiresAt));

        assertThatThrownBy(() -> publicTripService.open(TOKEN))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("41 (expired")
                .hasMessageNotContaining(TOKEN);
        verifyNoInteractions(tripDayRepository, activityRepository);
    }

    static Stream<Arguments> expiriesThatAreNoLongerAhead() {
        return Stream.of(
                // The same boundary as the expired flag of the owner's list
                Arguments.of("exactly now", NOW),
                Arguments.of("one microsecond ago", NOW.minus(1, ChronoUnit.MICROS)),
                Arguments.of("a week ago", NOW.minus(7, ChronoUnit.DAYS)));
    }

    @Test
    void aLiveLinkOfADeletedTripIs404AndNothingOfTheTripIsRead() {
        ReflectionTestUtils.setField(trip, "deletedAt", NOW.minus(1, ChronoUnit.DAYS));
        linkOfTheToken(link(null));

        assertThatThrownBy(() -> publicTripService.open(TOKEN))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Trip")
                .hasMessageNotContaining(TOKEN);
        verifyNoInteractions(tripDayRepository, activityRepository);
    }

    // ---- helpers -----------------------------------------------------------------------------------------------

    /** A VIEW link of the trip with the given expiry, live, as the database hands it back. */
    private ShareLink link(Instant expiresAt) {
        ShareLink link = ShareLink.builder()
                .trip(trip)
                .token(TOKEN)
                .expiresAt(expiresAt)
                .createdBy(owner)
                .build();
        ReflectionTestUtils.setField(link, "id", LINK_ID);
        return link;
    }

    private void linkOfTheToken(ShareLink link) {
        when(shareLinkRepository.findWithTripByToken(TOKEN)).thenReturn(Optional.of(link));
    }

    /** A day with a note: the note must never reach the page (decided 2026-10-13). */
    private TripDay day(long id, int dayIndex, LocalDate date, String title) {
        TripDay day = TripDay.builder().trip(trip).dayIndex(dayIndex).date(date).title(title)
                .note("Ghi chú riêng của ngày").build();
        ReflectionTestUtils.setField(day, "id", id);
        return day;
    }

    /** An activity with a cost and a booking link: neither may reach the page. */
    private Activity activity(TripDay day, String title, ActivityType type, LocalTime start, LocalTime end,
                              String note, Place place) {
        return Activity.builder()
                .tripDay(day)
                .title(title)
                .type(type)
                .startTime(start)
                .endTime(end)
                .note(note)
                .costAmount(new BigDecimal("150000.00"))
                .currency("VND")
                .bookingUrl("https://booking.example.com/private-reservation")
                .place(place)
                .createdBy(owner)
                .build();
    }

}
