-- V9: places (design.md 5.2). Mirrored by model/Place.java (ddl-auto=validate).
-- A place is the app's own copy ("snapshot") of something a map source returned, or a place a user added by
-- hand. Activities point here (Task 3.2, V10), so a trip page is drawn without calling the map source again.
-- One place of a source = one row, shared by everyone who picks it: UNIQUE (provider, external_id).

CREATE TABLE places (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    -- Every value the app will ever write is declared now; the Java enum grows task by task (MOCK, then MANUAL, then OSM)
    provider    ENUM('MOCK', 'OSM', 'MANUAL') NOT NULL,
    -- The id of the place inside its source. Compared byte for byte (utf8mb4_bin): an identifier is not text,
    -- "W123" and "w123" may be two different places. NULL for MANUAL places, which have no source
    external_id VARCHAR(128)   CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
    name        VARCHAR(200)   NOT NULL,
    address     VARCHAR(500)   NULL,
    lat         DECIMAL(10, 7) NOT NULL,
    lng         DECIMAL(10, 7) NOT NULL,
    -- Free text, not an ENUM: the mock data uses the activity types, a real source may send anything
    category    VARCHAR(40)    NULL,
    -- Who added a MANUAL place (rule 14.19: it is private to that user); NULL for places copied from a source
    created_by  BIGINT         NULL,
    created_at  DATETIME(6)    NOT NULL,
    updated_at  DATETIME(6)    NOT NULL,

    PRIMARY KEY (id),
    -- MySQL allows any number of rows with a NULL external_id, so MANUAL places never collide here
    UNIQUE KEY uk_places_provider_external_id (provider, external_id),
    KEY idx_places_created_by (created_by),
    -- No ON DELETE CASCADE: users are soft-deleted; a hard delete must not silently wipe places that activities use
    CONSTRAINT fk_places_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    -- Last line of defence; the friendly errors live in the request validation
    CONSTRAINT chk_places_lat CHECK (lat BETWEEN -90 AND 90),
    CONSTRAINT chk_places_lng CHECK (lng BETWEEN -180 AND 180)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
