package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.RefreshToken;
import com.trieu.tripplanner.model.enums.RevokedReason;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Closes every live session of the user in one statement (theft, password reset, blocked account;
     * design.md 6.1). Sessions revoked earlier keep their own time and reason.
     * clearAutomatically so entities already loaded in this transaction do not shadow the new revoked_at.
     *
     * @return number of sessions revoked
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken t SET t.revokedAt = :now, t.revokedReason = :reason
            WHERE t.user.id = :userId AND t.revokedAt IS NULL
            """)
    int revokeAllActiveByUserId(@Param("userId") Long userId, @Param("now") Instant now,
                                @Param("reason") RevokedReason reason);

    long countByUserIdAndRevokedAtIsNull(Long userId);

}
