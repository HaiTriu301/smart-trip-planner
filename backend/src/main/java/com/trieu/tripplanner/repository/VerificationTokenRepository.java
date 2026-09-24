package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.VerificationToken;
import com.trieu.tripplanner.model.enums.VerificationTokenType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByTokenHash(String tokenHash);

    /**
     * Issuing a new token of a type invalidates the older live ones of the same user and type
     * (design.md 5.2): only the most recent link in the inbox works.
     *
     * @return number of tokens invalidated
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VerificationToken t SET t.usedAt = :now "
            + "WHERE t.user.id = :userId AND t.type = :type AND t.usedAt IS NULL")
    int invalidateActive(@Param("userId") Long userId, @Param("type") VerificationTokenType type, @Param("now") Instant now);

    long countByUserIdAndTypeAndUsedAtIsNull(Long userId, VerificationTokenType type);

}
