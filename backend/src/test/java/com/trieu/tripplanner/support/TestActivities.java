package com.trieu.tripplanner.support;

import com.trieu.tripplanner.dto.request.CreateActivityRequest;
import com.trieu.tripplanner.dto.request.UpdateActivityRequest;
import com.trieu.tripplanner.dto.response.ActivityResponse;
import com.trieu.tripplanner.dto.response.PlaceResponse;
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

    /** Body of POST .../activities without a place; null means the field was not sent. */
    public static CreateActivityRequest createRequest(String title, ActivityType type, LocalTime startTime,
                                                      LocalTime endTime, String note, BigDecimal costAmount,
                                                      String currency, String bookingUrl) {
        return new CreateActivityRequest(title, type, startTime, endTime, note, costAmount, currency, bookingUrl,
                null);
    }

    /** The same body, asking for the activity to happen at the place {@code placeId}. */
    public static CreateActivityRequest withPlace(CreateActivityRequest request, Long placeId) {
        return new CreateActivityRequest(request.title(), request.type(), request.startTime(), request.endTime(),
                request.note(), request.costAmount(), request.currency(), request.bookingUrl(), placeId);
    }

    /** Body of PATCH .../activities/{id} that leaves the place alone; null means "keep the current value". */
    public static UpdateActivityRequest updateRequest(String title, ActivityType type, LocalTime startTime,
                                                      LocalTime endTime, String note, BigDecimal costAmount,
                                                      String currency, String bookingUrl) {
        return new UpdateActivityRequest(title, type, startTime, endTime, note, costAmount, currency, bookingUrl,
                null);
    }

    /** The same body, also moving the activity to the place {@code placeId}. */
    public static UpdateActivityRequest withPlace(UpdateActivityRequest request, Long placeId) {
        return new UpdateActivityRequest(request.title(), request.type(), request.startTime(), request.endTime(),
                request.note(), request.costAmount(), request.currency(), request.bookingUrl(), placeId);
    }

    /** An activity as the API returns it, without a place. */
    public static ActivityResponse response(Long id, Long dayId, String title, ActivityType type, LocalTime startTime,
                                            LocalTime endTime, int orderIndex, String note, BigDecimal costAmount,
                                            String currency, String bookingUrl, Long createdById, Long version,
                                            Instant createdAt, Instant updatedAt) {
        return new ActivityResponse(id, dayId, title, type, startTime, endTime, orderIndex, note, costAmount,
                currency, bookingUrl, null, createdById, version, createdAt, updatedAt);
    }

    /** The same activity, happening at {@code place}. */
    public static ActivityResponse withPlace(ActivityResponse activity, PlaceResponse place) {
        return new ActivityResponse(activity.id(), activity.dayId(), activity.title(), activity.type(),
                activity.startTime(), activity.endTime(), activity.orderIndex(), activity.note(),
                activity.costAmount(), activity.currency(), activity.bookingUrl(), place, activity.createdById(),
                activity.version(), activity.createdAt(), activity.updatedAt());
    }

}
