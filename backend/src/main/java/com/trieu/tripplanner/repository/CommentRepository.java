package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Comments of trips (design.md 5.2 "comments"). Comment carries {@code @SQLRestriction("deleted_at IS NULL")}, so
 * every query here sees live comments only.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

}
