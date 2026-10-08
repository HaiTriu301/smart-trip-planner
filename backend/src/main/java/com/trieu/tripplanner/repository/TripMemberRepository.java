package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.TripMember;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Members and pending invitations of trips (design.md 5.2 "trip_members"). TripMember has no soft delete, so
 * every query here sees every row, REMOVED ones included; callers filter on status themselves.
 */
public interface TripMemberRepository extends JpaRepository<TripMember, Long> {

    /**
     * The one row of an email on a trip, whatever its status (served by uk_trip_members_trip_email). Inviting
     * decides from it whether to create, re-send or refuse. The email must already be normalised (lowercase).
     */
    Optional<TripMember> findByTripIdAndInvitedEmail(Long tripId, String invitedEmail);

}
