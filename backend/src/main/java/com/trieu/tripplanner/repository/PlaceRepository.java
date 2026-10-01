package com.trieu.tripplanner.repository;

import com.trieu.tripplanner.model.Place;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Places copied from a map source (design.md 5.2 "places").
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {

    /**
     * The copy of one place of a source, if it was picked before. Served by uk_places_provider_external_id;
     * external_id is compared exactly, including case.
     */
    Optional<Place> findByProviderAndExternalId(PlaceProvider provider, String externalId);

}
