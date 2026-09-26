package com.trieu.tripplanner.security.permission;

import com.trieu.tripplanner.repository.TripRepository;
import com.trieu.tripplanner.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Trip permission checks used from {@code @PreAuthorize("@tripPermission.canEdit(#id, principal)")}
 * (design.md 6.2, CLAUDE.md rule 15). Services never repeat these checks.
 * <p>
 * Phase 2 knows only owners, so all three checks mean "is the owner". Task 4.2 adds members, share links
 * and the Redis cache behind the same method names, so controllers do not change.
 * <p>
 * A missing or deleted trip answers {@code true} on purpose: the request continues and the service throws
 * ResourceNotFoundException → 404 (design.md 6.2 "Triển khai theo giai đoạn"). An existing trip the caller
 * may not touch → {@code false} → 403.
 */
@Component("tripPermission")
@RequiredArgsConstructor
public class TripPermissionEvaluator {

    private final TripRepository tripRepository;

    /** View the trip and everything inside it. */
    public boolean canView(Long tripId, Object principal) {
        return isOwnerOrMissing(tripId, principal);
    }

    /** Edit trip info, days and activities. */
    public boolean canEdit(Long tripId, Object principal) {
        return isOwnerOrMissing(tripId, principal);
    }

    /** Owner-only actions: delete, members, share links, export. */
    public boolean isOwner(Long tripId, Object principal) {
        return isOwnerOrMissing(tripId, principal);
    }

    // principal is Object: SpEL passes the String "anonymousUser" when nobody is signed in
    private boolean isOwnerOrMissing(Long tripId, Object principal) {
        if (tripId == null || !(principal instanceof CustomUserDetails user)) {
            return false;
        }
        return tripRepository.findOwnerIdById(tripId)
                .map(ownerId -> ownerId.equals(user.getId()))
                .orElse(true);
    }

}
