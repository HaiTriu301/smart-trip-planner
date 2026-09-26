package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.request.UpdateTripRequest;
import com.trieu.tripplanner.dto.response.TripResponse;
import com.trieu.tripplanner.dto.response.TripSummaryResponse;
import com.trieu.tripplanner.model.Trip;
import org.mapstruct.BeanMapping;
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
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TripMapper {

    // owner.id reads the FK held by the lazy proxy; it does not load the user
    @Mapping(target = "ownerId", source = "owner.id")
    TripResponse toResponse(Trip trip);

    TripSummaryResponse toSummary(Trip trip);

    /**
     * PATCH semantics: null in the request keeps the current value. Only fields with a setter on Trip are
     * targets, so owner/slug/version can never be overwritten; status is excluded explicitly.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "status", ignore = true)
    void updateFromRequest(UpdateTripRequest request, @MappingTarget Trip trip);

}
