-- V5: trips (design.md 5.2). Mirrored 1:1 by model/Trip.java (ddl-auto=validate).
-- version backs @Version optimistic locking (design.md 11.3); deleted_at backs soft delete (design.md 14.8).

CREATE TABLE trips (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    owner_id         BIGINT         NOT NULL,
    title            VARCHAR(160)   NOT NULL,
    slug             VARCHAR(200)   NOT NULL,
    description      TEXT           NULL,
    cover_image_url  VARCHAR(512)   NULL,
    destination_name VARCHAR(200)   NULL,
    destination_lat  DECIMAL(10, 7) NULL,
    destination_lng  DECIMAL(10, 7) NULL,
    start_date       DATE           NOT NULL,
    end_date         DATE           NOT NULL,
    budget_amount    DECIMAL(15, 2) NULL,
    currency         CHAR(3)        NOT NULL DEFAULT 'VND',
    status           ENUM('DRAFT', 'PLANNED', 'ONGOING', 'COMPLETED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    visibility       ENUM('PRIVATE', 'LINK', 'PUBLIC')                            NOT NULL DEFAULT 'PRIVATE',
    version          BIGINT         NOT NULL DEFAULT 0,
    created_at       DATETIME(6)    NOT NULL,
    updated_at       DATETIME(6)    NOT NULL,
    deleted_at       DATETIME(6)    NULL,

    PRIMARY KEY (id),
    -- A UNIQUE key is itself an index, so it doubles as idx_trips_slug from design.md 5.2
    UNIQUE KEY uk_trips_slug (slug),
    -- Leftmost column owner_id also serves the foreign key below
    KEY idx_trips_owner_status (owner_id, status),
    -- No ON DELETE CASCADE: users are soft-deleted; a hard delete must not silently wipe their trips
    CONSTRAINT fk_trips_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    -- Last line of defence for design.md 14.1; the 60-day limit and the friendly error live in TripService
    CONSTRAINT chk_trips_date_range CHECK (end_date >= start_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
