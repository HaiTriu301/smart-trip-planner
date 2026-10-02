package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link User}. Every derived query inherits the soft-delete filter
 * ({@code @SQLRestriction}), so deleted accounts are invisible here without extra conditions.
 * Callers must pass the email already lowercased (the entity stores it that way).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Time zone of a live account without loading the entity; empty when the account does not exist or is
     * deleted. Asked on requests that need "today" for the caller, so it must stay a single cheap query.
     */
    @Query("select u.timezone from User u where u.id = :id")
    Optional<String> findTimezoneById(@Param("id") Long id);

}
