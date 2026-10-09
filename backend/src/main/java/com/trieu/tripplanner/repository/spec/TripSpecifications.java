package com.trieu.tripplanner.repository.spec;

import com.trieu.tripplanner.dto.internal.TripFilter;
import com.trieu.tripplanner.model.Trip;
import com.trieu.tripplanner.model.TripMember;
import com.trieu.tripplanner.model.enums.MemberStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Builds the WHERE clause of the trip list from {@link TripFilter}. Absent filters add no condition.
 * The access condition is always present: the user's own trips plus the trips shared with them, i.e. where they
 * are an ACCEPTED member (design.md 10.2 "Chuyến đi được chia sẻ trong danh sách"). A PENDING invitation or a
 * REMOVED membership shows nothing.
 */
public final class TripSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private TripSpecifications() {
    }

    public static Specification<Trip> matching(Long userId, TripFilter filter) {
        List<Specification<Trip>> specs = new ArrayList<>();
        specs.add(accessibleBy(userId));
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

    /**
     * Loads the owner in the same SELECT as the trips, so ownerName on every card costs no query per shared
     * trip. Only on a query that returns trips: the paging COUNT and the status grouping select something else,
     * and a fetch there would be an error. Adds no condition.
     */
    public static Specification<Trip> withOwner() {
        return (root, query, cb) -> {
            if (Trip.class.equals(query.getResultType())) {
                root.fetch("owner");
            }
            return null;
        };
    }

    /**
     * Own or shared: owner_id = user OR an ACCEPTED membership row of the user exists. A subquery rather than a
     * join, so a trip can never come out twice and the paging COUNT stays plain.
     */
    private static Specification<Trip> accessibleBy(Long userId) {
        return (root, query, cb) -> {
            Subquery<Long> membership = query.subquery(Long.class);
            Root<TripMember> member = membership.from(TripMember.class);
            membership.select(member.get("id")).where(
                    cb.equal(member.get("trip"), root),
                    cb.equal(member.get("user").get("id"), userId),
                    cb.equal(member.get("status"), MemberStatus.ACCEPTED));
            // owner.id is the FK column itself, so no join to users is generated for the first half
            return cb.or(cb.equal(root.get("owner").get("id"), userId), cb.exists(membership));
        };
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
