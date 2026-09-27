package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.request.UpdateTripDayRequest;
import com.trieu.tripplanner.dto.response.TripDayResponse;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.TripDayMapper;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.repository.TripDayRepository;
import com.trieu.tripplanner.repository.TripRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps one TripDay per calendar date of a trip (design.md 5.2 "trip_days", rules 14.2 and 14.3) and serves
 * the day endpoints. Plain class: the logic is small (CLAUDE.md rule 5). Permission is checked by
 * {@code @tripPermission} on the controller, never here (CLAUDE.md rule 15).
 */
@Service
@RequiredArgsConstructor
public class TripDayService {

    private static final String TRIP = "Trip";
    private static final String TRIP_DAY = "TripDay";

    private final TripDayRepository tripDayRepository;
    private final TripRepository tripRepository;
    private final TripDayMapper tripDayMapper;

    /**
     * Creates day 1..n for every date from start to end inclusive (rule 14.2). MANDATORY: it must run inside the
     * transaction that creates the trip, so a trip never exists without its days.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void generateDays(Trip trip) {
        AtomicInteger dayIndex = new AtomicInteger(1);
        List<TripDay> days = trip.getStartDate()
                .datesUntil(trip.getEndDate().plusDays(1))
                .map(date -> TripDay.builder()
                        .trip(trip)
                        .dayIndex(dayIndex.getAndIncrement())
                        .date(date)
                        .build())
                .toList();
        tripDayRepository.saveAll(days);
    }

    /**
     * Days of a live trip in calendar order.
     *
     * @throws ResourceNotFoundException the trip does not exist or is soft-deleted (404); the permission evaluator
     *                                   lets missing trips through on purpose so this answer is a 404, not a 403
     */
    @Transactional(readOnly = true)
    public List<TripDayResponse> list(Long tripId) {
        requireLiveTrip(tripId);
        return tripDayMapper.toResponses(tripDayRepository.findByTripIdOrderByDate(tripId));
    }

    /**
     * Edits the title / note of one day (design.md 10.2 "Quy ước sửa ngày"): null keeps, blank clears, other
     * values are trimmed. The date and dayIndex are never edited here: they follow the trip's dates.
     *
     * @throws ResourceNotFoundException the trip is missing or deleted, or the day belongs to another trip (404)
     */
    @Transactional
    public TripDayResponse update(Long tripId, Long dayId, UpdateTripDayRequest request) {
        requireLiveTrip(tripId);
        TripDay day = tripDayRepository.findByIdAndTripId(dayId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP_DAY, dayId));
        day.setTitle(applyText(request.title(), day.getTitle()));
        day.setNote(applyText(request.note(), day.getNote()));
        return tripDayMapper.toResponse(day);
    }

    // existsById goes through JPQL, so @SQLRestriction hides soft-deleted trips: their days are unreachable.
    // Needed because the permission evaluator lets missing trips through so that the answer is 404, not 403.
    private void requireLiveTrip(Long tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException(TRIP, tripId);
        }
    }

    /** null → keep current; blank → clear (NULL); otherwise the trimmed text. */
    private static String applyText(String incoming, String current) {
        if (incoming == null) {
            return current;
        }
        return incoming.isBlank() ? null : incoming.trim();
    }

}
