package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link User}. Every derived query inherits the soft-delete filter
 * ({@code @SQLRestriction}), so deleted accounts are invisible here without extra conditions.
 * Callers must pass the email already lowercased (the entity stores it that way).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}
