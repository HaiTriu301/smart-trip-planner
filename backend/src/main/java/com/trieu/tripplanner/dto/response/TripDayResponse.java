package com.trieu.tripplanner.dto.response;

import java.time.LocalDate;

/**
 * One day of a trip. The UI shows both parts, e.g. "Ngày 1 · Thứ Năm, 01/01/2026" (design.md 5.2 "trip_days").
 *
 * @param dayIndex 1-based position inside the trip
 * @param date     the calendar date
 * @param title    optional; null means the UI falls back to "Ngày {dayIndex}"
 */
public record TripDayResponse(
        Long id,
        int dayIndex,
        LocalDate date,
        String title,
        String note) {
}
