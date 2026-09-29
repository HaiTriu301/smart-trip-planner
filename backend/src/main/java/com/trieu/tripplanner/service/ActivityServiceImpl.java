package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.dto.request.CreateActivityRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.FieldViolation;
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
import java.util.List;
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
    public ActivityResponse create(Long tripId, Long dayId, Long userId, CreateActivityRequest request,
                                   boolean allowOverlap) {
        Trip trip = findLiveTrip(tripId);
        TripDay day = findDayOfTrip(dayId, tripId);

        LocalTime startTime = toMinutes(request.startTime());
        LocalTime endTime = toMinutes(request.endTime());
        validateTimeRange(startTime, endTime);
        if (!allowOverlap) {
            validateNoTimeConflict(dayId, startTime, endTime);
        }

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

    /**
     * design.md rule 14.4: two activities of one day must not overlap. Only activities with both times have a
     * range, so an activity without an end time is never checked and never blocks another one.
     * <p>
     * Compared in Java on purpose, see {@link ActivityRepository#findTimedByTripDayId}.
     */
    private void validateNoTimeConflict(Long dayId, LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return;
        }
        List<Activity> conflicts = activityRepository.findTimedByTripDayId(dayId).stream()
                .filter(other -> overlaps(other, startTime, endTime))
                .toList();
        if (conflicts.isEmpty()) {
            return;
        }
        // The earliest one is enough for the message; the client asks "add anyway?" and retries with allowOverlap
        Activity first = conflicts.getFirst();
        throw new BusinessRuleException(ErrorCode.ACTIVITY_TIME_CONFLICT,
                "Activity %s-%s overlaps %d activities of day %d, first is activity %d"
                        .formatted(startTime, endTime, conflicts.size(), dayId, first.getId()),
                List.of(FieldViolation.of("startTime", "error.activity.time-conflict-with",
                        first.getTitle(), first.getStartTime().toString(), first.getEndTime().toString())));
    }

    /**
     * Half-open ranges [start, end): each one starts before the other ends. Ranges that only touch
     * (09:00–10:00 and 10:00–11:00) do not overlap, so activities can follow each other without a gap.
     */
    static boolean overlaps(Activity other, LocalTime startTime, LocalTime endTime) {
        return other.getStartTime().isBefore(endTime) && other.getEndTime().isAfter(startTime);
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
