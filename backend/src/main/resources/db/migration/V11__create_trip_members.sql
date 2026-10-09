-- V11: trip_members (design.md 5.2). Mirrored 1:1 by model/TripMember.java (ddl-auto=validate).
-- One row per person invited to a trip. The owner has NO row here: trips.owner_id is the only source of truth
-- for ownership, so role only knows EDITOR and VIEWER (design.md 6.2, decision 2 of the Phase 4 review).
-- A row is never deleted by the app: removing a member sets status = REMOVED and inviting the same email
-- again reuses the row (decision 11). No deleted_at.

CREATE TABLE trip_members (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    trip_id           BIGINT       NOT NULL,
    -- NULL while the invited email has no account yet; set when the invitation is sent to an existing account
    -- or when the invitee accepts after registering (design.md 5.2)
    user_id           BIGINT       NULL,
    -- Lowercase, normalised like users.email, so the UNIQUE key below treats "A@x.com" and "a@x.com" as one invite
    invited_email     VARCHAR(255) NOT NULL,
    role              ENUM('EDITOR', 'VIEWER') NOT NULL,
    -- No DECLINED: there is no endpoint to decline yet; it is added with ALTER when one exists
    status            ENUM('PENDING', 'ACCEPTED', 'REMOVED') NOT NULL DEFAULT 'PENDING',
    -- SHA-256 hex of the one-time token in the invitation mail; the raw token is never stored (design.md 6.1).
    -- NULL once the invitation is accepted
    invite_token_hash CHAR(64)     NULL,
    invite_expires_at DATETIME(6)  NULL,
    invited_by        BIGINT       NOT NULL,
    invited_at        DATETIME(6)  NOT NULL,
    accepted_at       DATETIME(6)  NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    -- One row per account per trip. MySQL lets any number of rows have a NULL user_id, so several pending
    -- invitations to people without an account never collide here
    UNIQUE KEY uk_trip_members_trip_user (trip_id, user_id),
    -- One row per email per trip: re-inviting updates the row instead of adding one
    UNIQUE KEY uk_trip_members_trip_email (trip_id, invited_email),
    UNIQUE KEY uk_trip_members_invite_token_hash (invite_token_hash),
    -- "Trips shared with me" (GET /trips) and the permission check both ask "rows of user X with status Y"
    KEY idx_trip_members_user_status (user_id, status),
    KEY idx_trip_members_invited_by (invited_by),
    -- Members belong wholly to their trip: when the scheduler hard-deletes a trip (rule 14.8) its members go too
    CONSTRAINT fk_trip_members_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    -- No ON DELETE CASCADE: users are soft-deleted; a hard delete must not silently wipe memberships
    CONSTRAINT fk_trip_members_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_trip_members_invited_by FOREIGN KEY (invited_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
