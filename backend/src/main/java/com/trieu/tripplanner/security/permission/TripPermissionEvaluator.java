package com.trieu.tripplanner.security.permission;

import com.trieu.tripplanner.dto.internal.TripAccess;
import com.trieu.tripplanner.security.CustomUserDetails;
import java.util.Optional;
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
 * The stored row is trusted when it lets the caller in, never when it refuses: a refusal is read again from the
 * database before it becomes a 403. A trip deleted since the row was stored must answer 404 to everybody, the
 * stranger included (CLAUDE.md rule 15, BUG-SHARE-002). A 403 therefore costs one query more; in normal use the
 * interface never offers what would be refused.
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
        Long userId = user.getId();
        Optional<TripAccess> stored = tripAccessCache.find(tripId, userId);
        if (stored.isEmpty() || rule.test(stored.get(), userId)) {
            return true;
        }
        // A stored refusal may be out of date: never answer 403 on it alone
        return tripAccessCache.refresh(tripId, userId)
                .map(access -> rule.test(access, userId))
                .orElse(true);
    }

}
