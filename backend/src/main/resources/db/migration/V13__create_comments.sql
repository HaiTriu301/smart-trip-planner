-- V13: comments (design.md 5.2). Mirrored 1:1 by model/Comment.java (ddl-auto=validate).
-- One comment on a trip, or on one activity of it; a reply points at a root comment of the same trip (one level
-- only, checked in CommentService). Comments cannot be edited, hence no @Version (rule 27).
-- Soft delete: deleted_at is stamped, the row stays. Deleting a root stamps its replies too (one UPDATE).

CREATE TABLE comments (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    trip_id     BIGINT      NOT NULL,
    -- NULL = a comment on the whole trip; a reply carries the activity of its root
    activity_id BIGINT      NULL,
    -- The author
    user_id     BIGINT      NOT NULL,
    -- NULL = a root comment; otherwise the root comment this one replies to
    parent_id   BIGINT      NULL,
    -- At most 2000 characters, checked on the request; stored trimmed
    content     TEXT        NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    -- Every entity extends BaseEntity; a comment is never edited, so this stays equal to created_at
    updated_at  DATETIME(6) NOT NULL,
    -- NULL = live
    deleted_at  DATETIME(6) NULL,

    PRIMARY KEY (id),
    -- "The comments of trip X, oldest first"; leftmost column trip_id also serves the foreign key below
    KEY idx_comments_trip_created (trip_id, created_at),
    KEY idx_comments_activity (activity_id),
    KEY idx_comments_user (user_id),
    KEY idx_comments_parent (parent_id),
    -- Comments belong wholly to their trip: when the scheduler hard-deletes a trip (rule 14.8) they go too
    CONSTRAINT fk_comments_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    -- An activity is hard-deleted (design.md 5.2 "activities"); its comments go with it (rule 27)
    CONSTRAINT fk_comments_activity FOREIGN KEY (activity_id) REFERENCES activities (id) ON DELETE CASCADE,
    -- No ON DELETE CASCADE: users are soft-deleted; a hard delete must not silently wipe what they wrote
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id),
    -- One hard delete (trip, day, activity) reaches a root and its replies together; with the default RESTRICT
    -- MySQL would refuse to remove the root while a reply still points at it
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
