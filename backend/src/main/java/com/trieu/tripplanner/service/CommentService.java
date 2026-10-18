package com.trieu.tripplanner.service;

import com.trieu.tripplanner.dto.request.CreateCommentRequest;
import com.trieu.tripplanner.dto.response.CommentResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.CommentMapper;
import com.trieu.tripplanner.model.Activity;
import com.trieu.tripplanner.model.Comment;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.repository.ActivityRepository;
import com.trieu.tripplanner.repository.CommentRepository;
import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comments of a trip (design.md 10.2 "Quy ước Comment API", rule 27). Who may reach a trip at all is decided by
 * the controller guard ({@code tripPermission.canView}, CLAUDE.md rule 15); a trip that does not exist or was
 * soft-deleted is a 404 from here.
 * <p>
 * A plain class, no interface: the rules are few (CLAUDE.md rule 5). The content of a comment is personal writing,
 * so it never goes to the log; only ids do.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final String TRIP = "Trip";
    private static final String ACTIVITY = "Activity";

    private final CommentRepository commentRepository;
    private final TripRepository tripRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    /**
     * Posts a comment written by the signed-in user: on the whole trip, on one of its activities when the request
     * names one, or as a reply to a root comment of the trip.
     */
    @Transactional
    public CommentResponse create(Long tripId, Long userId, CreateCommentRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP, tripId));
        Comment parent = request.parentId() == null ? null : findRootOfTrip(request.parentId(), tripId);

        Comment saved = commentRepository.save(Comment.builder()
                .trip(trip)
                .activity(activityOf(request, parent, tripId))
                .parent(parent)
                .author(userRepository.getReferenceById(userId))
                .content(request.content().trim())
                .build());

        log.info("Trip {}: comment {} posted by user {}", tripId, saved.getId(), userId);
        return commentMapper.toResponse(saved);
    }

    // One level of replies (design.md 5.2): the parent must be a live root comment of this trip. Missing, deleted,
    // a reply, or of another trip all get the same answer, so nothing is learnt about the comments of other trips
    private Comment findRootOfTrip(Long parentId, Long tripId) {
        return commentRepository.findRootByIdAndTripId(parentId, tripId)
                .orElseThrow(() -> BusinessRuleException.invalidField("parentId", "error.comment.parent-not-root",
                        "Comment %d is not a live root comment of trip %d".formatted(parentId, tripId)));
    }

    // A reply sits where its parent sits, so the activityId sent with a reply is ignored (design.md 10.2)
    private Activity activityOf(CreateCommentRequest request, Comment parent, Long tripId) {
        if (parent != null) {
            return parent.getActivity();
        }
        return request.activityId() == null ? null : findActivityOfTrip(request.activityId(), tripId);
    }

    // Empty when the activity belongs to another trip: permission was checked on the trip of the URL only, so
    // without this a viewer of one trip could comment on an activity of any trip
    private Activity findActivityOfTrip(Long activityId, Long tripId) {
        return activityRepository.findByIdAndTripId(activityId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException(ACTIVITY, activityId));
    }

}
