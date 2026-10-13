-- V12: share_links (design.md 5.2). Mirrored 1:1 by model/ShareLink.java (ddl-auto=validate).
-- One row per link the owner of a trip created. Whoever holds the token may VIEW the trip without an account
-- (GET /api/v1/public/trips/{token}); a link never grants a write (design.md 6.2, rule 14.26).
-- A row is never deleted by the app: revoking a link stamps revoked_at and cannot be undone. No deleted_at.

CREATE TABLE share_links (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    trip_id    BIGINT       NOT NULL,
    -- 32 random characters of [a-z0-9]. Stored raw, unlike trip_members.invite_token_hash: the link exists to be
    -- handed around, it only lets people view, and the owner must be able to list and copy it again (design.md 5.2)
    token      CHAR(32)     NOT NULL,
    -- Phase 4 only has view links; EDIT is added with ALTER when edit links exist (decision 3 of the Phase 4 review)
    permission ENUM('VIEW') NOT NULL DEFAULT 'VIEW',
    -- NULL = the link never expires
    expires_at DATETIME(6)  NULL,
    -- NULL = the link is live; set once, by the owner
    revoked_at DATETIME(6)  NULL,
    -- Raised by one UPDATE ... SET view_count = view_count + 1 per successful opening of the public page
    view_count INT          NOT NULL DEFAULT 0,
    created_by BIGINT       NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    -- The public page looks a link up by its token on every request; also what keeps two links apart
    UNIQUE KEY uk_share_links_token (token),
    -- The owner's list: the links of one trip, newest first
    KEY idx_share_links_trip_created (trip_id, created_at),
    KEY idx_share_links_created_by (created_by),
    -- Links belong wholly to their trip: when the scheduler hard-deletes a trip (rule 14.8) its links go too
    CONSTRAINT fk_share_links_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    -- No ON DELETE CASCADE: users are soft-deleted; a hard delete must not silently wipe links
    CONSTRAINT fk_share_links_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
