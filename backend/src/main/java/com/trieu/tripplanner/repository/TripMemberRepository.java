package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.TripMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * The pending invitation behind a link, by the SHA-256 of the raw token (served by
     * uk_trip_members_invite_token_hash). Empty once accepted: the hash is cleared then.
     */
    Optional<TripMember> findByInviteTokenHash(String inviteTokenHash);

    /**
     * The people shown on a trip besides the owner (design.md 10.2 "Sharing", GET /members): accepted members
     * first, then pending invitations, each group oldest invitation first; REMOVED rows are hidden. The account
     * is fetched in the same query, so mapping the names costs no further SQL. The CASE is spelt out because
     * MySQL orders an ENUM column by position, not by name.
     */
    @Query("""
            select m from TripMember m
            left join fetch m.user
            where m.trip.id = :tripId and m.status <> com.trieu.tripplanner.model.enums.MemberStatus.REMOVED
            order by case when m.status = com.trieu.tripplanner.model.enums.MemberStatus.ACCEPTED then 0 else 1 end,
                     m.invitedAt, m.id
            """)
    List<TripMember> findActiveByTripId(@Param("tripId") Long tripId);

}
