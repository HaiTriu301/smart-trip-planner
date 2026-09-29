-- V7: activities (design.md 5.2). Mirrored 1:1 by model/Activity.java (ddl-auto=validate).
-- One thing to do inside a trip day. version backs @Version optimistic locking (design.md 11.3).
-- No deleted_at: activities are hard-deleted. No place_id yet: the places table arrives in Task 3.2,
-- which adds the column and its foreign key with its own migration.

CREATE TABLE activities (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    trip_day_id BIGINT         NOT NULL,
    title       VARCHAR(200)   NOT NULL,
    type        ENUM('SIGHTSEEING', 'FOOD', 'TRANSPORT', 'ACCOMMODATION', 'SHOPPING', 'OTHER') NOT NULL DEFAULT 'OTHER',
    -- Wall-clock time inside the day; the calendar date comes from trip_days.date
    start_time  TIME           NULL,
    end_time    TIME           NULL,
    order_index INT            NOT NULL,
    note        TEXT           NULL,
    cost_amount DECIMAL(15, 2) NULL,
    currency    CHAR(3)        NULL,
    booking_url VARCHAR(512)   NULL,
    created_by  BIGINT         NOT NULL,
    version     BIGINT         NOT NULL DEFAULT 0,
    created_at  DATETIME(6)    NOT NULL,
    updated_at  DATETIME(6)    NOT NULL,

    PRIMARY KEY (id),
    -- "Activities of day X in display order"; leftmost column trip_day_id also serves the foreign key below
    KEY idx_activities_day_order (trip_day_id, order_index),
    KEY idx_activities_created_by (created_by),
    -- Activities belong wholly to their day: cutting a day (force=true, rule 14.3) or hard-deleting the trip
    -- (trips -> trip_days -> activities) removes them too
    CONSTRAINT fk_activities_trip_day FOREIGN KEY (trip_day_id) REFERENCES trip_days (id) ON DELETE CASCADE,
    -- No ON DELETE CASCADE: users are soft-deleted; a hard delete must not silently wipe what they created
    CONSTRAINT fk_activities_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    -- Last line of defence; the friendly errors live in ActivityService.
    -- An end needs a start, and must come after it (no activity across midnight)
    CONSTRAINT chk_activities_time_range
        CHECK (end_time IS NULL OR (start_time IS NOT NULL AND end_time > start_time)),
    CONSTRAINT chk_activities_cost CHECK (cost_amount IS NULL OR cost_amount >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
