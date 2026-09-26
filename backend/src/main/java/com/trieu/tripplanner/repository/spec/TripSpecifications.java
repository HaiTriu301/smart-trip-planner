package com.trieu.tripplanner.repository.spec;

import com.trieu.tripplanner.dto.internal.TripFilter;
import com.trieu.tripplanner.model.Trip;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Builds the WHERE clause of the trip list from {@link TripFilter}. Absent filters add no condition.
 * The owner condition is always present: a user only ever lists their own trips (shared trips come in Phase 4).
 */
public final class TripSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private TripSpecifications() {
    }

    public static Specification<Trip> matching(Long ownerId, TripFilter filter) {
        List<Specification<Trip>> specs = new ArrayList<>();
        specs.add(ownedBy(ownerId));
        if (filter.status() != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (StringUtils.hasText(filter.q())) {
            specs.add(keyword(filter.q().trim()));
        }
        if (filter.from() != null) {
            specs.add(endsOnOrAfter(filter.from()));
        }
        if (filter.to() != null) {
            specs.add(startsOnOrBefore(filter.to()));
        }
        return Specification.allOf(specs);
    }

    private static Specification<Trip> ownedBy(Long ownerId) {
        // owner.id is the FK column itself, so no join to users is generated
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    /** Collation utf8mb4_unicode_ci already makes LIKE case-insensitive, so no lower() is needed. */
    private static Specification<Trip> keyword(String q) {
        String pattern = "%" + escapeLike(q) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(root.get("title"), pattern, LIKE_ESCAPE),
                cb.like(root.get("destinationName"), pattern, LIKE_ESCAPE));
    }

    // Overlap test: [start, end] intersects [from, to] ⇔ end >= from AND start <= to
    private static Specification<Trip> endsOnOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("endDate"), from);
    }

    private static Specification<Trip> startsOnOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), to);
    }

    /** A user typing "50%" must search for the literal text, not "50 followed by anything". */
    private static String escapeLike(String value) {
        return value
                .replace(String.valueOf(LIKE_ESCAPE), "" + LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
    }

}
