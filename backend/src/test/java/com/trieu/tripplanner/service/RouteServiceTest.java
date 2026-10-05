package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.dto.response.DayRouteResponse;
import com.trieu.tripplanner.dto.response.RouteLegResponse;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.model.Activity;
import com.trieu.tripplanner.model.Place;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.RouteLeg;
import com.trieu.tripplanner.repository.ActivityRepository;
import com.trieu.tripplanner.repository.TripDayRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.support.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The repositories and the route cache (the way to the map source) are mocks: the legs it answers are made up,
 * so these tests check what the service does with them, not how a distance is estimated (that is
 * MockMapProviderTest) nor what is kept in Redis (that is RouteCacheIntegrationTest).
 */
@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    private static final long TRIP_ID = 5L;
    private static final long DAY_ID = 11L;

    private static final Place CHO_HAN = place(31L, "Chợ Hàn", "16.0683525", "108.2242830");
    private static final Place CAU_RONG = place(32L, "Cầu Rồng", "16.0611370", "108.2275720");
    private static final Place LINH_UNG = place(33L, "Chùa Linh Ứng", "16.1001567", "108.2784112");

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private RouteCache routeCache;

    @InjectMocks
    private RouteService routeService;

    @Test
    void givesOneLegBetweenEveryTwoConsecutiveActivitiesAndTheTotalsOfTheDay() {
        dayHas(activity(101L, CHO_HAN), activity(102L, CAU_RONG), activity(103L, LINH_UNG));
        when(routeCache.legs(List.of(pointOf(CHO_HAN), pointOf(CAU_RONG), pointOf(LINH_UNG))))
                .thenReturn(List.of(new RouteLeg(1150, 138), new RouteLeg(8400, 1008)));

        DayRouteResponse route = routeService.forDay(TRIP_ID, DAY_ID);

        assertThat(route.legs()).containsExactly(
                new RouteLegResponse(101L, 102L, 1150, 138),
                new RouteLegResponse(102L, 103L, 8400, 1008));
        assertThat(route.totalDistanceMeters()).isEqualTo(9550);
        assertThat(route.totalDurationSeconds()).isEqualTo(1146);
    }

    @Test
    void asksTheMapForThePlacesInTheOrderTheDayShowsThem() {
        // The repository returns the display order (orderIndex, then id); ids are deliberately not ascending
        dayHas(activity(103L, LINH_UNG), activity(101L, CHO_HAN), activity(102L, CAU_RONG));
        when(routeCache.legs(List.of(pointOf(LINH_UNG), pointOf(CHO_HAN), pointOf(CAU_RONG))))
                .thenReturn(List.of(new RouteLeg(8000, 960), new RouteLeg(1150, 138)));

        DayRouteResponse route = routeService.forDay(TRIP_ID, DAY_ID);

        assertThat(route.legs()).containsExactly(
                new RouteLegResponse(103L, 101L, 8000, 960),
                new RouteLegResponse(101L, 102L, 1150, 138));
        // One question for the whole day, not one per leg
        verify(routeCache).legs(List.of(pointOf(LINH_UNG), pointOf(CHO_HAN), pointOf(CAU_RONG)));
    }

    @Test
    void activityWithoutAPlaceBetweenTwoPlacesIsSkippedAndItsNeighboursAreJoined() {
        dayHas(activity(101L, CHO_HAN), activity(102L, null), activity(103L, LINH_UNG));
        when(routeCache.legs(List.of(pointOf(CHO_HAN), pointOf(LINH_UNG))))
                .thenReturn(List.of(new RouteLeg(8900, 1068)));

        DayRouteResponse route = routeService.forDay(TRIP_ID, DAY_ID);

        // The way is not cut at 102: one leg from the place before it to the place after it
        assertThat(route.legs()).containsExactly(new RouteLegResponse(101L, 103L, 8900, 1068));
        assertThat(route.totalDistanceMeters()).isEqualTo(8900);
        assertThat(route.totalDurationSeconds()).isEqualTo(1068);
    }

    @Test
    void activitiesWithoutAPlaceAtTheStartAndTheEndOfTheDayAreSkipped() {
        dayHas(activity(100L, null), activity(101L, CHO_HAN), activity(102L, CAU_RONG), activity(103L, null),
                activity(104L, LINH_UNG), activity(105L, null));
        when(routeCache.legs(List.of(pointOf(CHO_HAN), pointOf(CAU_RONG), pointOf(LINH_UNG))))
                .thenReturn(List.of(new RouteLeg(1150, 138), new RouteLeg(8400, 1008)));

        DayRouteResponse route = routeService.forDay(TRIP_ID, DAY_ID);

        // One leg fewer than there are activities with a place, whatever stands around them
        assertThat(route.legs()).containsExactly(
                new RouteLegResponse(101L, 102L, 1150, 138),
                new RouteLegResponse(102L, 104L, 8400, 1008));
    }

    @Test
    void twoConsecutiveActivitiesAtTheSamePlaceKeepTheirLegOfZero() {
        dayHas(activity(101L, CHO_HAN), activity(102L, CHO_HAN), activity(103L, CAU_RONG));
        when(routeCache.legs(List.of(pointOf(CHO_HAN), pointOf(CHO_HAN), pointOf(CAU_RONG))))
                .thenReturn(List.of(new RouteLeg(0, 0), new RouteLeg(1150, 138)));

        DayRouteResponse route = routeService.forDay(TRIP_ID, DAY_ID);

        // Having a place is what counts, not moving: the UI hides a leg of zero if it wants to
        assertThat(route.legs()).containsExactly(
                new RouteLegResponse(101L, 102L, 0, 0),
                new RouteLegResponse(102L, 103L, 1150, 138));
        assertThat(route.totalDistanceMeters()).isEqualTo(1150);
    }

    @Test
    void dayWithoutActivitiesHasAnEmptyRouteAndTheMapIsNotAsked() {
        dayHas();

        assertThat(routeService.forDay(TRIP_ID, DAY_ID)).isEqualTo(new DayRouteResponse(List.of(), 0, 0));
        verifyNoInteractions(routeCache);
    }

    @Test
    void dayWithOnePlaceHasAnEmptyRouteAndTheMapIsNotAsked() {
        dayHas(activity(101L, CHO_HAN));

        // One stop is nowhere to travel to: a real routing service would answer an error to such a question
        assertThat(routeService.forDay(TRIP_ID, DAY_ID)).isEqualTo(new DayRouteResponse(List.of(), 0, 0));
        verifyNoInteractions(routeCache);
    }

    @Test
    void manyActivitiesButOnlyOneWithAPlaceIsStillAnEmptyRoute() {
        dayHas(activity(101L, null), activity(102L, CHO_HAN), activity(103L, null), activity(104L, null));

        // What counts is the number of places, not the number of activities
        assertThat(routeService.forDay(TRIP_ID, DAY_ID)).isEqualTo(new DayRouteResponse(List.of(), 0, 0));
        verifyNoInteractions(routeCache);
    }

    @Test
    void dayWithNoPlaceAtAllIsAnEmptyRoute() {
        dayHas(activity(101L, null), activity(102L, null));

        assertThat(routeService.forDay(TRIP_ID, DAY_ID)).isEqualTo(new DayRouteResponse(List.of(), 0, 0));
        verifyNoInteractions(routeCache);
    }

    @Test
    void missingTripIsNotFoundAndNothingIsRead() {
        when(tripRepository.existsById(TRIP_ID)).thenReturn(false);

        assertThatThrownBy(() -> routeService.forDay(TRIP_ID, DAY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(tripDayRepository, activityRepository, routeCache);
    }

    @Test
    void dayOfAnotherTripIsNotFoundAndItsActivitiesAreNeverRead() {
        when(tripRepository.existsById(TRIP_ID)).thenReturn(true);
        when(tripDayRepository.findByIdAndTripId(DAY_ID, TRIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.forDay(TRIP_ID, DAY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        // Permission was checked on the trip of the URL only: the day of another trip must stay unread
        verifyNoInteractions(activityRepository, routeCache);
    }

    /** An existing trip whose day DAY_ID holds these activities, in display order. */
    private void dayHas(Activity... activities) {
        when(tripRepository.existsById(TRIP_ID)).thenReturn(true);
        when(tripDayRepository.findByIdAndTripId(DAY_ID, TRIP_ID))
                .thenReturn(Optional.of(TripDay.builder().dayIndex(1).date(LocalDate.of(2026, 10, 5)).build()));
        when(activityRepository.findByTripDayIdOrderByOrderIndexAscIdAsc(DAY_ID)).thenReturn(List.of(activities));
    }

    /** @param place null: an activity that has no place */
    private static Activity activity(long id, Place place) {
        Activity activity = Activity.builder()
                .title("Hoạt động " + id)
                .orderIndex(1000)
                .place(place)
                .createdBy(TestUsers.verified(7L, "owner@example.com"))
                .build();
        ReflectionTestUtils.setField(activity, "id", id);
        return activity;
    }

    private static Place place(long id, String name, String lat, String lng) {
        Place place = Place.builder().provider(PlaceProvider.MOCK).externalId("place-" + id).name(name)
                .lat(new BigDecimal(lat)).lng(new BigDecimal(lng)).build();
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }

    private static Coordinate pointOf(Place place) {
        return new Coordinate(place.getLat(), place.getLng());
    }

}
