package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.Comment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Comments of trips (design.md 5.2 "comments"). Comment carries {@code @SQLRestriction("deleted_at IS NULL")}, so
 * every query here sees live comments only.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * A live root comment of the trip: the only thing a reply may point to (one level, design.md 5.2). Empty when
     * the comment does not exist, was deleted, is itself a reply, or belongs to another trip.
     */
    @Query("select c from Comment c where c.id = :id and c.trip.id = :tripId and c.parent is null")
    Optional<Comment> findRootByIdAndTripId(@Param("id") Long id, @Param("tripId") Long tripId);

}
