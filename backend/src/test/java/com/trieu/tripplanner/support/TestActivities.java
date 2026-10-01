package com.trieu.tripplanner.support;

import com.trieu.tripplanner.dto.request.CreateActivityRequest;
import com.trieu.tripplanner.dto.request.UpdateActivityRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.model.enums.ActivityType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/**
 * The one place where tests call the constructors of the activity DTOs. These records have no builder, so a new
 * field changes the constructor: with every test going through here, only this class has to follow, and tests
 * that do not care about the new field stay as they are.
 */
public final class TestActivities {

    private TestActivities() {
    }

    /** Body of POST .../activities; null means the field was not sent. */
    public static CreateActivityRequest createRequest(String title, ActivityType type, LocalTime startTime,
                                                      LocalTime endTime, String note, BigDecimal costAmount,
                                                      String currency, String bookingUrl) {
        return new CreateActivityRequest(title, type, startTime, endTime, note, costAmount, currency, bookingUrl);
    }

    /** Body of PATCH .../activities/{id}; null means "keep the current value". */
    public static UpdateActivityRequest updateRequest(String title, ActivityType type, LocalTime startTime,
                                                      LocalTime endTime, String note, BigDecimal costAmount,
                                                      String currency, String bookingUrl) {
        return new UpdateActivityRequest(title, type, startTime, endTime, note, costAmount, currency, bookingUrl);
    }

    /** An activity as the API returns it. */
    public static ActivityResponse response(Long id, Long dayId, String title, ActivityType type, LocalTime startTime,
                                            LocalTime endTime, int orderIndex, String note, BigDecimal costAmount,
                                            String currency, String bookingUrl, Long createdById, Long version,
                                            Instant createdAt, Instant updatedAt) {
        return new ActivityResponse(id, dayId, title, type, startTime, endTime, orderIndex, note, costAmount,
                currency, bookingUrl, createdById, version, createdAt, updatedAt);
    }

}
