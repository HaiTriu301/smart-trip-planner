package com.trieu.tripplanner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.TestcontainersConfiguration;
import com.trieu.tripplanner.model.Place;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Real MySQL: the lookup PlaceService uses to decide between "already stored" and "store it now".
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PlaceRepositoryTest {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findsTheStoredCopyOfAPlaceOfASource() {
        Place saved = placeRepository.saveAndFlush(place("da-nang-cho-han", "Chợ Hàn"));
        placeRepository.saveAndFlush(place("da-nang-cho-con", "Chợ Cồn"));
        entityManager.clear();

        assertThat(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "da-nang-cho-han"))
                .get()
                .satisfies(found -> {
                    assertThat(found.getId()).isEqualTo(saved.getId());
                    assertThat(found.getName()).isEqualTo("Chợ Hàn");
                });
    }

    @Test
    void isEmptyForAPlaceThatWasNeverPicked() {
        placeRepository.saveAndFlush(place("da-nang-cho-han", "Chợ Hàn"));

        assertThat(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "da-nang-cho-con")).isEmpty();
    }

    @Test
    void doesNotMatchAnExternalIdThatDiffersOnlyByCase() {
        // The database default collation ignores case; this column must not (V9: utf8mb4_bin)
        placeRepository.saveAndFlush(place("W123", "Một nơi"));

        assertThat(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "w123")).isEmpty();
        assertThat(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "W123")).isPresent();
    }

    private static Place place(String externalId, String name) {
        return Place.builder()
                .provider(PlaceProvider.MOCK)
                .externalId(externalId)
                .name(name)
                .lat(new BigDecimal("16.0683525"))
                .lng(new BigDecimal("108.2242830"))
                .build();
    }

}
