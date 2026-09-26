package com.trieu.tripplanner.service;

import com.trieu.tripplanner.common.PageResponse;
import com.trieu.tripplanner.common.util.SlugGenerator;
import com.trieu.tripplanner.dto.internal.TripFilter;
import com.trieu.tripplanner.dto.request.CreateTripRequest;
import com.trieu.tripplanner.dto.request.UpdateTripRequest;
import com.trieu.tripplanner.dto.response.TripResponse;
import com.trieu.tripplanner.dto.response.TripSummaryResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.exception.SlugGenerationException;
import com.trieu.tripplanner.mapper.TripMapper;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.enums.TripVisibility;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import com.trieu.tripplanner.repository.spec.TripSpecifications;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TripSummaryResponse> list(Long userId, TripFilter filter, Pageable pageable) {
        validateSort(pageable.getSort());
        return PageResponse.from(tripRepository
                .findAll(TripSpecifications.matching(userId, filter), pageable)
                .map(tripMapper::toSummary));
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
        log.info("Trip {} created by user {}", saved.getId(), userId);
        return tripMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse get(Long tripId) {
        return tripMapper.toResponse(findTrip(tripId));
    }

    @Override
    @Transactional
    public TripResponse update(Long tripId, UpdateTripRequest request) {
        Trip trip = findTrip(tripId);
        tripMapper.updateFromRequest(request, trip);
        if (request.title() != null) {
            trip.setTitle(request.title().trim());
        }
        // Checked after merging: PATCH {endDate} alone must still respect the stored startDate
        validateDateRange(trip.getStartDate(), trip.getEndDate());
        validateCoordinates(trip.getDestinationLat(), trip.getDestinationLng());

        // Flush now so the response carries the incremented version and updatedAt
        return tripMapper.toResponse(tripRepository.saveAndFlush(trip));
    }

    @Override
    @Transactional
    public void delete(Long tripId) {
        Trip trip = findTrip(tripId);
        tripRepository.delete(trip);
        log.info("Trip {} soft-deleted", tripId);
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
