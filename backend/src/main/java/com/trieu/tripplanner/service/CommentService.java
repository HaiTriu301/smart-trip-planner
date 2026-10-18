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
import java.util.List;
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

    /**
     * Every live comment of the trip, flat and oldest first; replies carry parentId so the client nests them one
     * level. With an activity, only the comments on that activity, replies included. No paging (design.md 10.2): a
     * trip of a small group rarely has more than a few hundred.
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long tripId, Long activityId) {
        // existsById goes through JPQL, so @SQLRestriction hides a soft-deleted trip: 404, like a missing one
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException(TRIP, tripId);
        }
        List<Comment> comments;
        if (activityId == null) {
            comments = commentRepository.findAllByTripIdWithAuthor(tripId);
        } else {
            // An activity of another trip is a 404, as when posting: an empty list would hide the mistake
            findActivityOfTrip(activityId, tripId);
            comments = commentRepository.findAllByTripIdAndActivityIdWithAuthor(tripId, activityId);
        }
        return comments.stream()
                .map(commentMapper::toResponse)
                .toList();
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
