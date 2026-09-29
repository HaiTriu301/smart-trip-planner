package com.trieu.tripplanner.dto.internal;

/**
 * What shortening a trip would destroy (design.md rule 14.3): the days that fall outside the new date range and
 * still hold activities. Filled by one aggregate query, so both numbers are 0 when nothing would be lost.
 *
 * @param days       number of cut days that hold at least one activity
 * @param activities number of activities inside those days
 */
public record DroppedActivities(long days, long activities) {

    public boolean isEmpty() {
        return activities == 0;
    }

}
