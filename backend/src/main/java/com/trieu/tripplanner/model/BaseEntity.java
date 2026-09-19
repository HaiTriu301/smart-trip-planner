package com.trieu.tripplanner.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Common columns shared by every table (design.md 4.2): surrogate id + audit timestamps.
 * Timestamps are filled by Hibernate on persist/update, so services never set them by hand.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Two entities are equal when they are the same row. Unsaved entities (id == null) are only equal to themselves,
     * which keeps Set/Map semantics stable before and after persist.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        BaseEntity that = (BaseEntity) other;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        // Constant per class so the hash does not change when the id is assigned on persist
        return getClass().hashCode();
    }

}
