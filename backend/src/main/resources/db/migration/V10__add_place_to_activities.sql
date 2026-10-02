-- V10: the place of an activity (design.md 5.2 "activities"). Mirrored by model/Activity.java (ddl-auto=validate).
-- An activity happens at no place or at one place; the place is a row of places (V9), shared by every activity
-- that uses it. Existing activities keep NULL: they were created before places existed.

ALTER TABLE activities
    ADD COLUMN place_id BIGINT NULL AFTER booking_url,
    ADD KEY idx_activities_place (place_id),
    -- No ON DELETE CASCADE in either direction: deleting an activity leaves the place for the others, and a
    -- place cannot be deleted while an activity still points to it
    ADD CONSTRAINT fk_activities_place FOREIGN KEY (place_id) REFERENCES places (id);
