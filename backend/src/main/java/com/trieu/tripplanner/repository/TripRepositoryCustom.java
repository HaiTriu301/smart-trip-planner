package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.dto.internal.TripStatusCount;
import com.trieu.tripplanner.model.Trip;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Queries of TripRepository that Spring Data cannot derive; implemented in TripRepositoryCustomImpl. */
public interface TripRepositoryCustom {

    /**
     * Number of trips per status among the trips matching the specification, in one grouped query.
     * Taking the list's own Specification keeps both filters identical (owner, keyword, soft delete).
     */
    List<TripStatusCount> countByStatus(Specification<Trip> spec);

}
