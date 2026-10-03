package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.response.DayRouteResponse;
import com.trieu.tripplanner.dto.response.RouteLegResponse;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.model.Activity;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.RouteLeg;
import com.trieu.tripplanner.repository.ActivityRepository;
import com.trieu.tripplanner.repository.TripDayRepository;
import com.trieu.tripplanner.repository.TripRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Travel between the activities of a day (design.md 10.2 "Quy ước Route"). Reaches the map source only through
 * its port: switching from the made-up estimate to a real routing service changes configuration, not this class
 * (CLAUDE.md rule 19). Simple enough to be a class without interface (CLAUDE.md rule 5).
 */
@Service
@RequiredArgsConstructor
public class RouteService {

    private static final String TRIP = "Trip";
    private static final String TRIP_DAY = "TripDay";

    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final ActivityRepository activityRepository;
    private final MapProvider mapProvider;

    /**
     * One leg between every two consecutive activities of the day, in the order the day shows them, and the
     * totals of the day. The activities are travelled as they are ordered; nothing is rearranged to shorten the
     * way.
     * <p>
     * Not @Transactional on purpose. Each repository call is one short read, and the activities come with their
     * places already loaded; a surrounding transaction would keep a database connection busy for as long as the
     * map source takes to answer, and a real source is a network call.
     *
     * @throws ResourceNotFoundException the trip does not exist or is deleted, or the day is not a day of that
     *                                   trip (404)
     */
    public DayRouteResponse forDay(Long tripId, Long dayId) {
        // Permission was checked on the trip of the URL; the evaluator lets a missing trip through so that the
        // answer is 404, not 403
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException(TRIP, tripId);
        }
        // Empty when the day belongs to another trip: without this, a viewer of one trip could read any day
        tripDayRepository.findByIdAndTripId(dayId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP_DAY, dayId));

        List<Activity> stops = activityRepository.findByTripDayIdOrderByOrderIndexAscIdAsc(dayId);
        List<Coordinate> points = stops.stream()
                .map(stop -> new Coordinate(stop.getPlace().getLat(), stop.getPlace().getLng()))
                .toList();
        return toResponse(stops, mapProvider.route(points));
    }

    /** Leg i of the source goes from stop i to stop i + 1: the source knows points, the ids are put back here. */
    private static DayRouteResponse toResponse(List<Activity> stops, List<RouteLeg> travel) {
        List<RouteLegResponse> legs = new ArrayList<>();
        long totalDistanceMeters = 0;
        long totalDurationSeconds = 0;
        for (int i = 0; i < travel.size(); i++) {
            RouteLeg leg = travel.get(i);
            legs.add(new RouteLegResponse(stops.get(i).getId(), stops.get(i + 1).getId(),
                    leg.distanceMeters(), leg.durationSeconds()));
            totalDistanceMeters += leg.distanceMeters();
            totalDurationSeconds += leg.durationSeconds();
        }
        return new DayRouteResponse(List.copyOf(legs), totalDistanceMeters, totalDurationSeconds);
    }

}
