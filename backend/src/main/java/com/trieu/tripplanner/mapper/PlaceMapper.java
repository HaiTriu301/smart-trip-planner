package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.response.PlaceResultResponse;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Provider result → DTO, generated at compile time, so the API never exposes a type of the provider layer.
 * unmappedTargetPolicy=ERROR: a new response field without a source fails the build.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PlaceMapper {

    PlaceResultResponse toResultResponse(PlaceResult result);

    List<PlaceResultResponse> toResultResponses(List<PlaceResult> results);

}
