package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.dto.internal.TripAccess;
import com.trieu.tripplanner.model.Trip;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Every JPQL/derived query here sees live trips only: @SQLRestriction on Trip appends "deleted_at IS NULL".
 * List filtering goes through {@link JpaSpecificationExecutor} with repository/spec/TripSpecifications.
 */
public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip>, TripRepositoryCustom {

    /**
     * What a user is to a live trip, for TripPermissionEvaluator on every protected request, so it must stay a
     * single cheap query: the owner id, plus the role of the user's ACCEPTED membership through an outer join
     * that matches at most one row (UNIQUE trip_id + user_id). Empty when the trip does not exist or is deleted;
     * present with a null role for the owner, a stranger, a PENDING or a REMOVED member. Nothing is loaded into
     * the persistence context.
     */
    @Query("""
            select new com.trieu.tripplanner.dto.internal.TripAccess(t.owner.id, m.role)
            from Trip t
            left join TripMember m on m.trip = t and m.user.id = :userId
                and m.status = com.trieu.tripplanner.model.enums.MemberStatus.ACCEPTED
            where t.id = :tripId
            """)
    Optional<TripAccess> findAccess(@Param("tripId") Long tripId, @Param("userId") Long userId);

    /**
     * The trip and its owner in one SELECT, for the detail page: the member list names the owner, so loading
     * them separately would be a second query on every GET /trips/{id}. Live trips only (@SQLRestriction).
     */
    @Query("select t from Trip t join fetch t.owner where t.id = :id")
    Optional<Trip> findWithOwnerById(@Param("id") Long id);

    /**
     * Native on purpose: soft-deleted rows still hold their slug in the UNIQUE key, and a JPQL query would
     * not see them because of @SQLRestriction.
     */
    @Query(value = "SELECT COUNT(*) FROM trips WHERE slug = :slug", nativeQuery = true)
    long countBySlugIncludingDeleted(@Param("slug") String slug);

}
