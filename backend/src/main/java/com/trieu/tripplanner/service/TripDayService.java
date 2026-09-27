package com.trieu.tripplanner.service;

import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.repository.TripDayRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps one TripDay per calendar date of a trip (design.md 5.2 "trip_days", rules 14.2 and 14.3).
 * Plain class: the logic is small (CLAUDE.md rule 5).
 */
@Service
@RequiredArgsConstructor
public class TripDayService {

    private final TripDayRepository tripDayRepository;

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

}
