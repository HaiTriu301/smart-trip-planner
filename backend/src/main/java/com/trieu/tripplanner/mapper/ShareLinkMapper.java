package com.trieu.tripplanner.mapper;

import com.trieu.tripplanner.dto.response.ShareLinkResponse;
import com.trieu.tripplanner.model.ShareLink;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * ShareLink → DTO, generated at compile time. unmappedTargetPolicy=ERROR: a new response field without a source
 * fails the build. The trip and the creator are deliberately not mapped, so neither is loaded.
 * <p>
 * {@code expired} is not a column: it depends on the moment of the call. The service passes that moment in as
 * {@code now} (read from the Clock bean), so every link of one answer is judged against the same instant.
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ShareLinkMapper {

    @Mapping(target = "expired", expression = "java(isExpired(link, now))")
    ShareLinkResponse toResponse(ShareLink link, @Context Instant now);

    List<ShareLinkResponse> toResponses(List<ShareLink> links, @Context Instant now);

    /**
     * A link without an expiry never expires. One with an expiry is expired from that very instant on: the same
     * boundary as at creation, where an expiry that is not after "now" is refused.
     */
    default boolean isExpired(ShareLink link, Instant now) {
        return link.getExpiresAt() != null && !link.getExpiresAt().isAfter(now);
    }

}
