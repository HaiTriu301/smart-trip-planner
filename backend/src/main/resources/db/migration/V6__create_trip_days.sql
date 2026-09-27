-- V6: trip_days (design.md 5.2). Mirrored 1:1 by model/TripDay.java (ddl-auto=validate).
-- One row per calendar date of a trip, always consecutive from trips.start_date to trips.end_date (rule 14.2, 14.3).
-- No deleted_at: a soft-deleted row would keep holding (trip_id, date) and block extending the trip back onto that date.

CREATE TABLE trip_days (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    trip_id    BIGINT       NOT NULL,
    day_index  INT          NOT NULL,
    date       DATE         NOT NULL,
    title      VARCHAR(160) NULL,
    note       TEXT         NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    -- Leftmost column trip_id also serves the foreign key below and "days of trip X ordered by date"
    UNIQUE KEY uk_trip_days_trip_date (trip_id, date),
    -- Days belong wholly to their trip: when the scheduler hard-deletes a trip (rule 14.8) its days go too
    CONSTRAINT fk_trip_days_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE,
    -- day_index is 1-based ("Ngày 1"); renumbering is done by TripDayService, this only guards against bugs
    CONSTRAINT chk_trip_days_day_index CHECK (day_index >= 1)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
