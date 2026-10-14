package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.response.PublicTripResponse;
import com.trieu.tripplanner.dto.response.PublicTripResponse.PublicActivity;
import com.trieu.tripplanner.dto.response.PublicTripResponse.PublicDay;
import com.trieu.tripplanner.dto.response.PublicTripResponse.PublicPlace;
import com.trieu.tripplanner.model.Activity;
import com.trieu.tripplanner.model.Place;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripDay;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Trip, days, activities and places → the public page, generated at compile time. unmappedTargetPolicy=ERROR: a
 * new field of PublicTripResponse without a source fails the build. The response only has the fields a guest may
 * see, so nothing else of the entities is read (the owner, the creator of an activity are never loaded).
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PublicTripMapper {

    /** Two sources: the trip and its days, already converted and in calendar order. */
    @Mapping(target = "days", source = "days")
    PublicTripResponse toResponse(Trip trip, List<PublicDay> days);

    /** Two sources: the day and its activities, already in display order. */
    @Mapping(target = "activities", source = "activities")
    PublicDay toDay(TripDay day, List<Activity> activities);

    PublicActivity toActivity(Activity activity);

    PublicPlace toPlace(Place place);

}
