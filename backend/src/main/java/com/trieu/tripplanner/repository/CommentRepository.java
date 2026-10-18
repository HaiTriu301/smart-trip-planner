package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.Comment;
import java.util.List;
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

    /**
     * Every live comment of the trip, roots and replies in one flat list, oldest first; the id breaks ties of the
     * same instant so the order never changes between two reads (design.md 10.2). The author comes in the same
     * query, so the number of SQL statements does not grow with the number of comments; activity and parent are
     * read as ids only, which needs no join.
     */
    @Query("select c from Comment c join fetch c.author where c.trip.id = :tripId order by c.createdAt, c.id")
    List<Comment> findAllByTripIdWithAuthor(@Param("tripId") Long tripId);

}
