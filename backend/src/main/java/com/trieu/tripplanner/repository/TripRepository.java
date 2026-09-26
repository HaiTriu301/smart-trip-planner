package com.trieu.tripplanner.repository;

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
public interface TripRepository extends JpaRepository<Trip, Long>, JpaSpecificationExecutor<Trip> {

    /**
     * Owner of a live trip without loading the entity; empty when the trip does not exist or is deleted.
     * Used by TripPermissionEvaluator on every protected request, so it must stay a single cheap query.
     */
    @Query("select t.owner.id from Trip t where t.id = :id")
    Optional<Long> findOwnerIdById(@Param("id") Long id);

    /**
     * Native on purpose: soft-deleted rows still hold their slug in the UNIQUE key, and a JPQL query would
     * not see them because of @SQLRestriction.
     */
    @Query(value = "SELECT COUNT(*) FROM trips WHERE slug = :slug", nativeQuery = true)
    long countBySlugIncludingDeleted(@Param("slug") String slug);

}
