package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.request.UpdateTripRequest;
import com.trieu.tripplanner.dto.response.MemberResponse;
import com.trieu.tripplanner.dto.response.TripDayDetailResponse;
import com.trieu.tripplanner.dto.response.TripDetailResponse;
import com.trieu.tripplanner.dto.response.TripResponse;
import com.trieu.tripplanner.dto.response.TripRole;
import com.trieu.tripplanner.dto.response.TripSummaryResponse;
import com.trieu.tripplanner.model.Trip;
import java.util.List;
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

    /**
     * activityCount comes from ActivityRepository.countByTripIds: one grouped query for the whole page.
     * owner.fullName reads the owner, which the list query fetched together with the trips (no query per card).
     */
    @Mapping(target = "ownerId", source = "trip.owner.id")
    @Mapping(target = "ownerName", source = "trip.owner.fullName")
    TripSummaryResponse toSummary(Trip trip, long activityCount);

    /**
     * Four sources: the trip, its days with their activities (TripDayService, one query per table), the member
     * list (SharingService, owner first) and the caller's role. Everything but the trip arrives as DTOs, so this
     * mapper needs no other mapper.
     */
    @Mapping(target = "ownerId", source = "trip.owner.id")
    @Mapping(target = "days", source = "days")
    @Mapping(target = "members", source = "members")
    @Mapping(target = "myRole", source = "myRole")
    TripDetailResponse toDetail(Trip trip, List<TripDayDetailResponse> days, List<MemberResponse> members,
                                TripRole myRole);

    /**
     * PATCH semantics: null in the request keeps the current value. Only fields with a setter on Trip are
     * targets, so owner/slug/version can never be overwritten; status is excluded explicitly.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "status", ignore = true)
    void updateFromRequest(UpdateTripRequest request, @MappingTarget Trip trip);

}
