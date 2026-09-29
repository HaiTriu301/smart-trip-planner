package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.request.CreateActivityRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.ActivityMapper;
import com.trieu.tripplanner.model.Activity;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import com.trieu.tripplanner.model.enums.ActivityType;
import com.trieu.tripplanner.repository.ActivityRepository;
import com.trieu.tripplanner.repository.TripDayRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    /** design.md 14.5: indexes are spaced so that an insert in between renumbers nothing. */
    static final int ORDER_STEP = 1000;

    private static final String TRIP = "Trip";
    private static final String TRIP_DAY = "TripDay";

    private final ActivityRepository activityRepository;
    private final TripRepository tripRepository;
    private final TripDayRepository tripDayRepository;
    private final UserRepository userRepository;
    private final ActivityMapper activityMapper;

    @Override
    @Transactional
    public ActivityResponse create(Long tripId, Long dayId, Long userId, CreateActivityRequest request) {
        Trip trip = findLiveTrip(tripId);
        TripDay day = findDayOfTrip(dayId, tripId);

        LocalTime startTime = toMinutes(request.startTime());
        LocalTime endTime = toMinutes(request.endTime());
        validateTimeRange(startTime, endTime);

        Activity activity = Activity.builder()
                .tripDay(day)
                .title(request.title().trim())
                .type(request.type() != null ? request.type() : ActivityType.OTHER)
                .startTime(startTime)
                .endTime(endTime)
                // Two simultaneous inserts may read the same maximum and share an index; the list breaks the tie
                // by id, and the reorder endpoint (Task 2.4) rewrites indexes anyway
                .orderIndex(activityRepository.findMaxOrderIndexByTripDayId(dayId) + ORDER_STEP)
                .note(blankToNull(request.note()))
                .costAmount(request.costAmount())
                .currency(resolveCurrency(request.currency(), request.costAmount(), trip))
                .bookingUrl(request.bookingUrl())
                // A reference is enough for the FK: no SELECT on users
                .createdBy(userRepository.getReferenceById(userId))
                .build();

        Activity saved = activityRepository.save(activity);
        log.info("Activity {} added to day {} of trip {} by user {}", saved.getId(), dayId, tripId, userId);
        return activityMapper.toResponse(saved);
    }

    // findById goes through JPQL, so @SQLRestriction hides soft-deleted trips. Needed because the permission
    // evaluator lets missing trips through so that the answer is 404, not 403. The entity (not just existsById)
    // is loaded because the currency of the trip is the default for a cost.
    private Trip findLiveTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP, tripId));
    }

    // Empty when the day belongs to another trip: permission was checked on the trip of the URL only
    private TripDay findDayOfTrip(Long dayId, Long tripId) {
        return tripDayRepository.findByIdAndTripId(dayId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP_DAY, dayId));
    }

    /**
     * design.md 5.2 "activities": an end needs a start and must come after it. No activity across midnight:
     * 23:00 → 01:00 is two activities on two days.
     */
    private static void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (endTime == null) {
            return;
        }
        if (startTime == null) {
            throw BusinessRuleException.invalidField("startTime", "error.activity.start-time-required",
                    "Activity has end time %s but no start time".formatted(endTime));
        }
        if (!endTime.isAfter(startTime)) {
            throw BusinessRuleException.invalidField("endTime", "error.activity.end-not-after-start",
                    "Activity end time %s is not after start time %s".formatted(endTime, startTime));
        }
    }

    /** Times are shown as HH:mm (design.md 10.1), so seconds sent by a client are dropped, not stored. */
    private static LocalTime toMinutes(LocalTime time) {
        return time == null ? null : time.truncatedTo(ChronoUnit.MINUTES);
    }

    /** A cost without a currency is counted in the currency of the trip; without a cost the value sent is kept. */
    private static String resolveCurrency(String currency, BigDecimal costAmount, Trip trip) {
        if (currency != null) {
            return currency;
        }
        return costAmount != null ? trip.getCurrency() : null;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

}
