package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.response.ShareLinkResponse;
import com.trieu.tripplanner.model.ShareLink;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * ShareLink → DTO, generated at compile time. unmappedTargetPolicy=ERROR: a new response field without a source
 * fails the build. The trip and the creator are deliberately not mapped, so neither is loaded.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ShareLinkMapper {

    ShareLinkResponse toResponse(ShareLink link);

}
