package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.PageResponse;
import com.trieu.tripplanner.common.util.SlugGenerator;
import com.trieu.tripplanner.dto.internal.TripActivityCount;
import com.trieu.tripplanner.dto.internal.TripFilter;
import com.trieu.tripplanner.dto.internal.TripStatusCount;
import com.trieu.tripplanner.dto.request.CreateTripRequest;
import com.trieu.tripplanner.dto.request.UpdateTripRequest;
import com.trieu.tripplanner.dto.response.TripDetailResponse;
import com.trieu.tripplanner.dto.response.TripResponse;
import com.trieu.tripplanner.dto.response.TripStatusCountsResponse;
import com.trieu.tripplanner.dto.response.TripSummaryResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.exception.SlugGenerationException;
import com.trieu.tripplanner.mapper.TripMapper;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.enums.TripStatus;
import com.trieu.tripplanner.model.enums.TripVisibility;
import com.trieu.tripplanner.repository.ActivityRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.repository.spec.TripSpecifications;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    /** design.md 14.1: both ends included, so at most 60 TripDays. */
    static final int MAX_TRIP_DAYS = 60;
    static final int SLUG_ATTEMPTS = 5;

    /**
     * Whitelist: sorting on any other property would either 500 (unknown path) or let a client probe
     * columns it cannot read (e.g. owner fields) through the result order.
     */
    static final List<String> SORTABLE_PROPERTIES = List.of("createdAt", "updatedAt", "startDate", "title");

    private static final String TRIP = "Trip";

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripMapper tripMapper;
    private final SlugGenerator slugGenerator;
    private final TripDayService tripDayService;
    private final ActivityRepository activityRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TripSummaryResponse> list(Long userId, TripFilter filter, Pageable pageable) {
        validateSort(pageable.getSort());
        Page<Trip> page = tripRepository.findAll(TripSpecifications.matching(userId, filter), pageable);
        Map<Long, Long> activityCounts = countActivities(page.getContent());
        return PageResponse.from(page.map(trip -> tripMapper.toSummary(trip, activityCounts.getOrDefault(trip.getId(), 0L))));
    }

    @Override
    @Transactional(readOnly = true)
    public TripStatusCountsResponse countByStatus(Long userId, String q) {
        Map<TripStatus, Long> counts = new EnumMap<>(TripStatus.class);
        for (TripStatus status : TripStatus.values()) {
            counts.put(status, 0L);
        }
        // Same filter as the list, status left open: the chips show every status
        TripFilter filter = new TripFilter(null, q, null, null);
        for (TripStatusCount row : tripRepository.countByStatus(TripSpecifications.matching(userId, filter))) {
            counts.put(row.status(), row.count());
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new TripStatusCountsResponse(total, counts);
    }

    @Override
    @Transactional
    public TripResponse create(Long userId, CreateTripRequest request) {
        validateDateRange(request.startDate(), request.endDate());
        validateCoordinates(request.destinationLat(), request.destinationLng());

        Trip trip = Trip.builder()
                // A reference is enough for the FK: no SELECT on users
                .owner(userRepository.getReferenceById(userId))
                .title(request.title().trim())
                .slug(uniqueSlug(request.title()))
                .description(request.description())
                .coverImageUrl(request.coverImageUrl())
                .destinationName(request.destinationName())
                .destinationLat(request.destinationLat())
                .destinationLng(request.destinationLng())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .budgetAmount(request.budgetAmount())
                .currency(request.currency() != null ? request.currency() : Trip.DEFAULT_CURRENCY)
                .visibility(request.visibility() != null ? request.visibility() : TripVisibility.PRIVATE)
                .build();

        Trip saved = tripRepository.save(trip);
        // Same transaction: if a day fails to insert, the trip is rolled back too (rule 14.2)
        tripDayService.generateDays(saved);
        log.info("Trip {} created by user {}", saved.getId(), userId);
        return tripMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TripDetailResponse get(Long tripId) {
        Trip trip = findTrip(tripId);
        // Days and activities by trip id, one query each, instead of lazy collections per trip and per day
        return tripMapper.toDetail(trip, tripDayService.listWithActivities(tripId));
    }

    @Override
    @Transactional
    public TripResponse update(Long tripId, UpdateTripRequest request, boolean force) {
        Trip trip = findTrip(tripId);
        LocalDate oldStart = trip.getStartDate();
        LocalDate oldEnd = trip.getEndDate();
        tripMapper.updateFromRequest(request, trip);
        if (request.title() != null) {
            trip.setTitle(request.title().trim());
        }
        // Checked after merging: PATCH {endDate} alone must still respect the stored startDate
        validateDateRange(trip.getStartDate(), trip.getEndDate());
        validateCoordinates(trip.getDestinationLat(), trip.getDestinationLng());

        // Same transaction: the trip's new dates and its days are committed together (rule 14.3)
        if (!trip.getStartDate().equals(oldStart) || !trip.getEndDate().equals(oldEnd)) {
            tripDayService.reconcileDays(trip, oldStart, oldEnd, force);
        }

        // Flush now so the response carries the incremented version and updatedAt
        return tripMapper.toResponse(tripRepository.saveAndFlush(trip));
    }

    @Override
    @Transactional
    public TripResponse updateStatus(Long tripId, TripStatus status) {
        Trip trip = findTrip(tripId);
        TripStatus previous = trip.getStatus();
        trip.setStatus(status);
        // Same status: Hibernate finds nothing dirty, so no UPDATE and the version stays
        TripResponse response = tripMapper.toResponse(tripRepository.saveAndFlush(trip));
        log.info("Trip {} status {} -> {}", tripId, previous, status);
        return response;
    }

    @Override
    @Transactional
    public void delete(Long tripId) {
        Trip trip = findTrip(tripId);
        tripRepository.delete(trip);
        log.info("Trip {} soft-deleted", tripId);
    }

    /** One grouped query for the whole page; none at all for an empty page. */
    private Map<Long, Long> countActivities(List<Trip> trips) {
        if (trips.isEmpty()) {
            return Map.of();
        }
        return activityRepository.countByTripIds(trips.stream().map(Trip::getId).toList()).stream()
                .collect(Collectors.toMap(TripActivityCount::tripId, TripActivityCount::count));
    }

    private Trip findTrip(Long tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP, tripId));
    }

    private static void validateDateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw BusinessRuleException.invalidField("endDate", "error.trip.end-before-start",
                    "Trip end date %s is before start date %s".formatted(end, start));
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_TRIP_DAYS) {
            throw BusinessRuleException.invalidField("endDate", "error.trip.too-long",
                    "Trip spans %d days, max %d".formatted(days, MAX_TRIP_DAYS), MAX_TRIP_DAYS);
        }
    }

    /** A map marker needs both values; one alone is meaningless (design.md 10.2 "Quy ước Trip API"). */
    private static void validateCoordinates(BigDecimal lat, BigDecimal lng) {
        if ((lat == null) != (lng == null)) {
            String missing = (lat == null) ? "destinationLat" : "destinationLng";
            throw BusinessRuleException.invalidField(missing, "error.trip.coordinates-incomplete",
                    "Only one of destinationLat/destinationLng is set");
        }
    }

    private static void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
                throw BusinessRuleException.invalidField("sort", "error.trip.sort-unsupported",
                        "Unsupported sort property " + order.getProperty(),
                        String.join(", ", SORTABLE_PROPERTIES));
            }
        }
    }

    /** Soft-deleted trips still own their slug in the UNIQUE key, hence the native count. */
    private String uniqueSlug(String title) {
        for (int attempt = 0; attempt < SLUG_ATTEMPTS; attempt++) {
            String candidate = slugGenerator.generate(title);
            if (tripRepository.countBySlugIncludingDeleted(candidate) == 0) {
                return candidate;
            }
        }
        throw new SlugGenerationException(SLUG_ATTEMPTS);
    }

}
