package com.trieu.tripplanner.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trieu.tripplanner.model.enums.ActivityType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * A trip as anybody holding a share link sees it, without an account (design.md 10.2 "Trang công khai").
 * Only what a guest needs to follow the itinerary. Deliberately absent: the id of any row, the owner (id, name,
 * email), the members, the budget and the costs, booking links, the notes of the days, the status and the
 * visibility. A field added here is published to the whole internet: check design.md 10.2 first.
 *
 * @param days every day of the trip in calendar order, with an empty list for a day without activities
 */
public record PublicTripResponse(
        String title,
        String description,
        String destinationName,
        BigDecimal destinationLat,
        BigDecimal destinationLng,
        LocalDate startDate,
        LocalDate endDate,
        List<PublicDay> days) {

    /**
     * @param activities in display order, as in the itinerary of the owner
     */
    public record PublicDay(
            int dayIndex,
            LocalDate date,
            String title,
            List<PublicActivity> activities) {
    }

    /**
     * @param startTime written as "HH:mm" (design.md 10.1); null = not scheduled
     * @param note      the note of the activity is shown; the note of the day is not (decided 2026-10-13)
     * @param place     null = no place
     */
    public record PublicActivity(
            String title,
            ActivityType type,
            @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            String note,
            PublicPlace place) {
    }

    /** Enough to show the place and put it on a map; no id, no source, no creator. */
    public record PublicPlace(
            String name,
            String address,
            BigDecimal lat,
            BigDecimal lng) {
    }

}
