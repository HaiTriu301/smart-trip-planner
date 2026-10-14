package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.response.PublicTripResponse;
import com.trieu.tripplanner.dto.response.PublicTripResponse.PublicDay;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.PublicTripMapper;
import com.trieu.tripplanner.model.Activity;
import com.trieu.tripplanner.model.ShareLink;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.repository.ActivityRepository;
import com.trieu.tripplanner.repository.ShareLinkRepository;
import com.trieu.tripplanner.repository.TripDayRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The public page of a trip, opened by the token of a share link without an account (design.md 10.2 "Trang công
 * khai", rule 14.26). Not guarded by the permission evaluator: the link row alone decides, and the trip's
 * visibility label is never read.
 * <p>
 * Every refusal (malformed or unknown token, revoked or expired link, deleted trip) is the same 404, so a guest
 * cannot tell a token that never existed from one that was switched off. The reason only goes to the log through
 * the exception message, which names the link by its id and never carries the token (CLAUDE.md rule 17).
 */
@Service
@RequiredArgsConstructor
public class PublicTripService {

    /** What SecureTokens.alphanumeric draws for a link (design.md 5.2 "share_links": CHAR(32)). */
    private static final Pattern TOKEN_FORMAT = Pattern.compile("[a-z0-9]{32}");

    private static final String SHARE_LINK = "ShareLink";
    private static final String TRIP = "Trip";

    private final ShareLinkRepository shareLinkRepository;
    private final TripDayRepository tripDayRepository;
    private final ActivityRepository activityRepository;
    private final PublicTripMapper publicTripMapper;
    private final Clock clock;

    /**
     * The trip of a live link with all its days and activities. Three queries whatever the size of the trip: the
     * link with its trip, the days, the activities with their places (grouped by day in memory, no N+1).
     *
     * @throws ResourceNotFoundException the token is malformed or unknown, the link is revoked or expired, or the
     *                                   trip was deleted (404, the same answer for all)
     */
    @Transactional(readOnly = true)
    public PublicTripResponse open(String token) {
        Trip trip = openableLink(token).getTrip();
        // groupingBy keeps the order of the query inside each day
        Map<Long, List<Activity>> activitiesByDay = activityRepository.findByTripIdInDisplayOrder(trip.getId())
                .stream()
                .collect(Collectors.groupingBy(activity -> activity.getTripDay().getId()));
        List<PublicDay> days = tripDayRepository.findByTripIdOrderByDate(trip.getId()).stream()
                .map(day -> publicTripMapper.toDay(day, activitiesByDay.getOrDefault(day.getId(), List.of())))
                .toList();
        return publicTripMapper.toResponse(trip, days);
    }

    private ShareLink openableLink(String token) {
        // Before the database: the _ci collation of the column would let an upper-case token find a lower-case one
        if (token == null || !TOKEN_FORMAT.matcher(token).matches()) {
            throw new ResourceNotFoundException(SHARE_LINK, "<malformed token>");
        }
        ShareLink link = shareLinkRepository.findWithTripByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(SHARE_LINK, "<unknown token>"));
        if (link.getRevokedAt() != null) {
            throw new ResourceNotFoundException(SHARE_LINK, "%d (revoked at %s)".formatted(link.getId(),
                    link.getRevokedAt()));
        }
        // Expired from that very instant on: the same boundary as the expired flag of the owner's list
        Instant now = clock.instant();
        if (link.getExpiresAt() != null && !link.getExpiresAt().isAfter(now)) {
            throw new ResourceNotFoundException(SHARE_LINK, "%d (expired at %s)".formatted(link.getId(),
                    link.getExpiresAt()));
        }
        if (link.getTrip().isDeleted()) {
            throw new ResourceNotFoundException(TRIP, "%d (deleted, reached through share link %d)".formatted(
                    link.getTrip().getId(), link.getId()));
        }
        return link;
    }

}
