package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.TripMember;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Members and pending invitations of trips (design.md 5.2 "trip_members"). TripMember has no soft delete, so
 * every query here sees every row, REMOVED ones included; callers filter on status themselves.
 */
public interface TripMemberRepository extends JpaRepository<TripMember, Long> {

}
