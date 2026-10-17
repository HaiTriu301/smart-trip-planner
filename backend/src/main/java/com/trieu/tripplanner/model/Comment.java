package com.trieu.tripplanner.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * A comment on a trip, or on one activity of it (design.md 5.2 "comments", rule 27). Column types must match
 * V13__create_comments.sql exactly: ddl-auto=validate refuses to start otherwise.
 * <p>
 * A reply points at a root comment ({@code parent == null}) of the same trip and carries the activity of that root;
 * replies go one level deep only. Both rules are checked in CommentService, not here.
 * <p>
 * A comment cannot be edited (rule 27): every column is fixed when it is written and not updatable, so there is no
 * setter and no {@code @Version}. It ends by being soft-deleted (CLAUDE.md rule 10): {@code @SQLRestriction} hides
 * deleted rows from every query, and {@code @SQLDelete} turns an accidental {@code repository.delete()} into a
 * stamp of {@code deleted_at} on that one row instead of removing it.
 */
@Entity
@Table(name = "comments")
@SQLDelete(sql = "UPDATE comments SET deleted_at = NOW(6) WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Comment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false, updatable = false)
    private Trip trip;

    /** Null = a comment on the whole trip. Hard-deleting the activity removes the comment (ON DELETE CASCADE). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", updatable = false)
    private Activity activity;

    /** Who wrote it: always the signed-in user, never taken from the request body (CLAUDE.md rule 16). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User author;

    /** Null = a root comment; otherwise the root comment this one replies to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", updatable = false)
    private Comment parent;

    // TEXT in MySQL: without columnDefinition, validate expects VARCHAR(255)
    @Column(name = "content", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String content;

    /** Set by the soft-delete statements only; null means the comment is live. */
    @Column(name = "deleted_at", updatable = false)
    private Instant deletedAt;

}
