package com.trieu.tripplanner.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.Range;

/**
 * Body of PUT /api/v1/trips/{tripId}/activities/reorder (design.md 10.2 "Quy ước Reorder"): the activities the
 * user dragged, each with the day and the position it was dropped on. Activities that did not move are not sent.
 * <p>
 * An object around the list, not a bare JSON array: a field can be added later without breaking old clients.
 * Only shape rules live here (CLAUDE.md rule 13); "every id belongs to the trip" and "no activity twice" need
 * the database or the whole list, so ActivityService checks them.
 *
 * @param items 1 to 200 moves, applied together or not at all
 */
public record ReorderActivitiesRequest(
        @NotEmpty(message = "{validation.reorder.items.required}")
        @Size(max = 200, message = "{validation.reorder.items.too-many}")
        List<@NotNull(message = "{validation.reorder.item.required}") @Valid Item> items) {

    /**
     * One move.
     *
     * @param dayId      the day the activity ends up in; its current day for a move inside the day
     * @param orderIndex the new position; between two neighbours 1000 and 2000 the client sends 1500 (rule 14.5)
     */
    public record Item(
            @NotNull(message = "{validation.reorder.activity-id.required}")
            Long activityId,

            @NotNull(message = "{validation.reorder.day-id.required}")
            Long dayId,

            @NotNull(message = "{validation.reorder.order-index.required}")
            @Range(min = 1, max = 1_000_000_000, message = "{validation.reorder.order-index.range}")
            Integer orderIndex) {
    }

}
