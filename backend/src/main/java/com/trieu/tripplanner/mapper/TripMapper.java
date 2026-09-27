package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.request.UpdateTripRequest;
import com.trieu.tripplanner.dto.response.TripDetailResponse;
import com.trieu.tripplanner.dto.response.TripResponse;
import com.trieu.tripplanner.dto.response.TripSummaryResponse;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Trip ↔ DTO mapping, generated at compile time. unmappedTargetPolicy=ERROR: a new response field without a
 * source fails the build. Creation is done by TripService with the entity builder instead, because a mapper
 * would pass null into currency/visibility and wipe their @Builder.Default values.
 */
// CONSTRUCTOR: the generated impl receives TripDayMapper through its constructor, not an @Autowired field
// (CLAUDE.md rule 4), and unit tests can build it with new TripMapperImpl(new TripDayMapperImpl())
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR, uses = TripDayMapper.class,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface TripMapper {

    // owner.id reads the FK held by the lazy proxy; it does not load the user
    @Mapping(target = "ownerId", source = "owner.id")
    TripResponse toResponse(Trip trip);

    TripSummaryResponse toSummary(Trip trip);

    /**
     * Two sources: the trip and its days, loaded separately by the service (1 query each, no N+1).
     * The day list is converted element by element with TripDayMapper ({@code uses}).
     */
    @Mapping(target = "ownerId", source = "trip.owner.id")
    @Mapping(target = "days", source = "days")
    TripDetailResponse toDetail(Trip trip, List<TripDay> days);

    /**
     * PATCH semantics: null in the request keeps the current value. Only fields with a setter on Trip are
     * targets, so owner/slug/version can never be overwritten; status is excluded explicitly.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "status", ignore = true)
    void updateFromRequest(UpdateTripRequest request, @MappingTarget Trip trip);

}
