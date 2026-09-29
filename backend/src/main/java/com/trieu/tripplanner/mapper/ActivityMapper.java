package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.model.Activity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Activity → DTO, generated at compile time. unmappedTargetPolicy=ERROR: a new response field without a source
 * fails the build. Creation is done by ActivityService with the entity builder, because a mapper would pass null
 * into type and wipe its @Builder.Default value.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivityMapper {

    // Both ids are read from the foreign keys held by the lazy proxies; neither trip_days nor users is loaded
    @Mapping(target = "dayId", source = "tripDay.id")
    @Mapping(target = "createdById", source = "createdBy.id")
    ActivityResponse toResponse(Activity activity);

    List<ActivityResponse> toResponses(List<Activity> activities);

}
