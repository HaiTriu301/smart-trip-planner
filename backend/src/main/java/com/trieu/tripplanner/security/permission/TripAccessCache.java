package com.trieu.tripplanner.security.permission;

import com.trieu.tripplanner.dto.internal.TripAccess;
import com.trieu.tripplanner.repository.TripRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Where {@link TripPermissionEvaluator} learns what an account is to a trip (design.md 6.2 "Triển khai theo giai
 * đoạn", 8.1 "trip:permission"). A bean of its own between the evaluator and {@link TripRepository#findAccess},
 * on the pattern of PlaceSearchCache: a cache works through a proxy Spring puts in front of a bean, so it only
 * sees calls that come from another bean, never a method of the evaluator calling itself (CLAUDE.md section 8).
 */
@Component
@RequiredArgsConstructor
public class TripAccessCache {

    private final TripRepository tripRepository;

    /**
     * The owner of the trip and the role of the account's ACCEPTED membership, read in one query.
     *
     * @return empty when the trip does not exist or is deleted
     */
    public Optional<TripAccess> find(Long tripId, Long userId) {
        return tripRepository.findAccess(tripId, userId);
    }

}
