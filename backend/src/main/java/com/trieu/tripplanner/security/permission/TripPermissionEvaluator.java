package com.trieu.tripplanner.security.permission;

import com.trieu.tripplanner.dto.internal.TripAccess;
import com.trieu.tripplanner.security.CustomUserDetails;
import java.util.function.BiPredicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Trip permission checks used from {@code @PreAuthorize("@tripPermission.canEdit(#id, principal)")}
 * (design.md 6.2, CLAUDE.md rule 15). Services never repeat these checks.
 * <p>
 * Since Task 4.1 the three checks read one row, {@link TripAccess}: who owns the trip and which role, if any,
 * the caller holds as an ACCEPTED member. PENDING and REMOVED members get no row role and are strangers.
 * Share links never come here: they only open the public endpoint. The row comes through {@link TripAccessCache},
 * so a cache in front of the query changes neither these methods nor the controllers.
 * <p>
 * A missing or deleted trip answers {@code true} on purpose: the request continues and the service throws
 * ResourceNotFoundException → 404 (design.md 6.2 "Triển khai theo giai đoạn"). An existing trip the caller
 * may not touch → {@code false} → 403.
 */
@Component("tripPermission")
@RequiredArgsConstructor
public class TripPermissionEvaluator {

    private final TripAccessCache tripAccessCache;

    /** View the trip and everything inside it: owner or any accepted member. */
    public boolean canView(Long tripId, Object principal) {
        return allowedOrMissing(tripId, principal, TripAccess::canView);
    }

    /** Edit trip info, status, days and activities: owner or accepted EDITOR. */
    public boolean canEdit(Long tripId, Object principal) {
        return allowedOrMissing(tripId, principal, TripAccess::canEdit);
    }

    /** Owner-only actions: delete, members, share links, export. */
    public boolean isOwner(Long tripId, Object principal) {
        return allowedOrMissing(tripId, principal, TripAccess::isOwner);
    }

    // principal is Object: SpEL passes the String "anonymousUser" when nobody is signed in
    private boolean allowedOrMissing(Long tripId, Object principal, BiPredicate<TripAccess, Long> rule) {
        if (tripId == null || !(principal instanceof CustomUserDetails user)) {
            return false;
        }
        return tripAccessCache.find(tripId, user.getId())
                .map(access -> rule.test(access, user.getId()))
                .orElse(true);
    }

}
