package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.dto.response.TripDayDetailResponse;
import com.trieu.tripplanner.dto.response.TripDayResponse;
import com.trieu.tripplanner.model.TripDay;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * TripDay → DTO, generated at compile time. unmappedTargetPolicy=ERROR: a new response field without a source
 * fails the build. The trip association is deliberately not mapped, so it is never loaded.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TripDayMapper {

    TripDayResponse toResponse(TripDay day);

    List<TripDayResponse> toResponses(List<TripDay> days);

    /**
     * Two sources: the day and its activities, already converted and ordered by the service. Taking DTOs keeps
     * this mapper free of other mappers, so it still has a no-argument constructor.
     */
    @Mapping(target = "activities", source = "activities")
    TripDayDetailResponse toDetail(TripDay day, List<ActivityResponse> activities);

}
