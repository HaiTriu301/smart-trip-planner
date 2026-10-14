package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.ShareLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Share links of trips (design.md 5.2 "share_links"). ShareLink has no soft delete, so every query here sees
 * every row, revoked and expired ones included; callers decide what those mean.
 */
public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    /**
     * Whether a token is already in use by any link, revoked ones included (served by uk_share_links_token).
     * Asked before a new link is stored, so a collision is answered by drawing again instead of by a failed
     * INSERT.
     */
    boolean existsByToken(String token);

    /**
     * The links of a trip that were not revoked, newest first; expired ones are among them. Two links created in
     * the same microsecond come out by id, so the order never changes between two calls. Served by
     * idx_share_links_trip_created; reads share_links only (neither the trip nor the creator is joined).
     */
    List<ShareLink> findByTripIdAndRevokedAtIsNullOrderByCreatedAtDescIdDesc(Long tripId);

    /**
     * A link addressed by the owner (revoke): the id must belong to the trip of the URL, so a link of another
     * trip is simply "not found". Revoked or not; the service decides what a revoked one means.
     */
    Optional<ShareLink> findByIdAndTripId(Long id, Long tripId);

    /**
     * Whether the trip has a link that was not revoked, apart from the given one. Asked while that link is being
     * revoked: leaving it out by id makes the answer the same whether or not its new revoked_at has reached the
     * database yet. An expired link that was never revoked counts (design.md 10.2: the trip keeps its LINK label).
     */
    boolean existsByTripIdAndRevokedAtIsNullAndIdNot(Long tripId, Long id);

    /**
     * The link of a token together with its trip, in one query: the first statement of the public page. Revoked
     * and expired links are returned too; the caller decides, so it can log why a link was refused. The trip may
     * come back soft-deleted or the row may not come back at all, depending on whether Hibernate applies the
     * trip's @SQLRestriction to the join; the caller treats both as "not found".
     * <p>
     * The token must have been checked against [a-z0-9]{32} first: the _ci collation of the column would let an
     * upper-case token find a lower-case one.
     */
    @Query("select l from ShareLink l join fetch l.trip where l.token = :token")
    Optional<ShareLink> findWithTripByToken(@Param("token") String token);

}
