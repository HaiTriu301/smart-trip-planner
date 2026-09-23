package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Theft response (design.md 6.1): kill every live session of the user in one statement.
     * clearAutomatically so entities already loaded in this transaction do not shadow the new revoked_at.
     *
     * @return number of sessions revoked
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.user.id = :userId AND t.revokedAt IS NULL")
    int revokeAllActiveByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    long countByUserIdAndRevokedAtIsNull(Long userId);

}
