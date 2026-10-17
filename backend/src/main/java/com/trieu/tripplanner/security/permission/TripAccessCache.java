package com.trieu.tripplanner.security.permission;

import com.trieu.tripplanner.common.constant.CacheNames;
import com.trieu.tripplanner.dto.internal.TripAccess;
import com.trieu.tripplanner.repository.TripRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Where {@link TripPermissionEvaluator} learns what an account is to a trip (design.md 6.2 "Triển khai theo giai
 * đoạn", 8.1 "trip:permission"). A bean of its own between the evaluator and {@link TripRepository#findAccess},
 * on the pattern of PlaceSearchCache: a cache works through a proxy Spring puts in front of a bean, so it only
 * sees calls that come from another bean, never a method of the evaluator calling itself (CLAUDE.md section 8).
 * <p>
 * The answer is kept in Redis for 5 minutes under {@code {userId}:{tripId}}, so the requests a trip page sends
 * one after the other ask the database once. The sharing service removes the entry of an account whose access it
 * changes ({@link #evict}). A trip deleted after its entry was stored needs no removal: a stored "allowed" lets the
 * request reach the service, which answers 404 as for any deleted trip; a stored refusal is never trusted alone,
 * the evaluator reads it again through {@link #refresh} (BUG-SHARE-002).
 */
@Component
@RequiredArgsConstructor
public class TripAccessCache {

    private final TripRepository tripRepository;

    /**
     * The owner of the trip and the role of the account's ACCEPTED membership, from Redis when asked in the last
     * 5 minutes, otherwise read in one query and stored.
     * <p>
     * "No such trip" is never stored (design.md 6.2 "Chốt 2026-10-16"). The evaluator lets a missing trip through
     * so the service can answer 404; stored, that answer would still let the caller through once someone creates a
     * trip with that id, and ids come in order, so the next one is easy to guess.
     *
     * @return empty when the trip does not exist or is deleted
     */
    @Cacheable(cacheNames = CacheNames.TRIP_PERMISSION, key = "#userId + ':' + #tripId", unless = "#result == null")
    public Optional<TripAccess> find(Long tripId, Long userId) {
        return tripRepository.findAccess(tripId, userId);
    }

    /**
     * Reads the access again from the database and stores it in place of the old entry, for a check the stored
     * entry refused. A trip deleted since is not stored either: the old entry stays until it expires, and each
     * check on that trip reads the database again.
     *
     * @return empty when the trip does not exist or is deleted
     */
    @CachePut(cacheNames = CacheNames.TRIP_PERMISSION, key = "#userId + ':' + #tripId", unless = "#result == null")
    public Optional<TripAccess> refresh(Long tripId, Long userId) {
        return tripRepository.findAccess(tripId, userId);
    }

    /** Forgets what is stored for this account on this trip: the next check reads the database again. */
    @CacheEvict(cacheNames = CacheNames.TRIP_PERMISSION, key = "#userId + ':' + #tripId")
    public void evict(Long tripId, Long userId) {
        // Nothing to do here: the annotation removes the entry
    }

}
