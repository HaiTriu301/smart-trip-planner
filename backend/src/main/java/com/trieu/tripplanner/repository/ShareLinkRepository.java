package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Share links of trips (design.md 5.2 "share_links"). ShareLink has no soft delete, so every query here sees
 * every row, revoked and expired ones included; callers decide what those mean.
 */
public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
}
