package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.dto.internal.TripStatusCount;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.enums.TripStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * Spring Data picks this class up as the implementation of TripRepositoryCustom (name + "Impl").
 * JpaSpecificationExecutor can count but not group, hence the hand-written criteria query.
 */
@RequiredArgsConstructor
class TripRepositoryCustomImpl implements TripRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public List<TripStatusCount> countByStatus(Specification<Trip> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TripStatusCount> query = cb.createQuery(TripStatusCount.class);
        Root<Trip> trip = query.from(Trip.class);
        Path<TripStatus> status = trip.get("status");
        query.select(cb.construct(TripStatusCount.class, status, cb.count(trip))).groupBy(status);
        // @SQLRestriction (soft delete) applies to criteria queries too, like to the list
        Predicate where = spec.toPredicate(trip, query, cb);
        if (where != null) {
            query.where(where);
        }
        return entityManager.createQuery(query).getResultList();
    }

}
