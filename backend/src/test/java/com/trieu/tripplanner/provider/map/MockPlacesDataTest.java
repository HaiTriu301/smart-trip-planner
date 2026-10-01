package com.trieu.tripplanner.provider.map;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.common.util.VietnameseText;
import com.trieu.tripplanner.model.enums.ActivityType;
import com.trieu.tripplanner.provider.map.MockMapProvider.MockPlace;
import com.trieu.tripplanner.provider.map.MockMapProvider.MockPlaceFile;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

/**
 * Checks the bundled data itself, not the code: a typo in mock/places.json (swapped coordinates, a duplicated
 * id, an unknown category) would otherwise only show up as a marker in the sea or a broken form.
 */
class MockPlacesDataTest {

    /** Each destination of the file: the prefix of its external ids and the centre of the town. */
    private static final Map<String, Coordinate> DESTINATIONS = Map.of(
            "da-nang-", point("16.0544", "108.2022"),
            "ha-noi-", point("21.0285", "105.8542"),
            "hoi-an-", point("15.8801", "108.3380"),
            "da-lat-", point("11.9404", "108.4583"),
            "ho-chi-minh-", point("10.7769", "106.7009"));

    private static final int MIN_PLACES_PER_DESTINATION = 8;
    private static final double MAX_DISTANCE_FROM_CENTRE_METERS = 40_000;

    private static MockPlaceFile file;

    @BeforeAll
    static void readTheFile() throws IOException {
        try (InputStream in = new ClassPathResource(MockMapProvider.DATA_FILE).getInputStream()) {
            file = new ObjectMapper().readValue(in, MockPlaceFile.class);
        }
    }

    @Test
    void fileCreditsOpenStreetMap() {
        // The coordinates come from OpenStreetMap: its licence asks for this notice to travel with the data
        assertThat(file.source()).contains("OpenStreetMap contributors", "ODbL");
    }

    @Test
    void everyPlaceHasAnIdANameAnAddressAndBothCoordinates() {
        assertThat(file.places()).allSatisfy(place -> {
            assertThat(place.externalId()).as("externalId").matches("[a-z0-9]+(-[a-z0-9]+)+");
            assertThat(place.name()).as("name of %s", place.externalId()).isNotBlank();
            assertThat(place.address()).as("address of %s", place.externalId()).isNotBlank();
            assertThat(place.lat()).as("lat of %s", place.externalId()).isNotNull();
            assertThat(place.lng()).as("lng of %s", place.externalId()).isNotNull();
        });
    }

    @Test
    void externalIdsAndNamesAreUnique() {
        assertThat(file.places()).extracting(MockPlace::externalId).doesNotHaveDuplicates();
        // Two rows with the same name would be indistinguishable in the suggestion list
        assertThat(file.places()).extracting(MockPlace::name).doesNotHaveDuplicates();
    }

    @Test
    void categoriesAreActivityTypesSoTheFormCanPreselectTheType() {
        Set<String> activityTypes = Arrays.stream(ActivityType.values()).map(Enum::name).collect(Collectors.toSet());

        assertThat(file.places()).allSatisfy(place ->
                assertThat(place.category()).as("category of %s", place.externalId()).isIn(activityTypes));
    }

    @Test
    void everyPlaceBelongsToOneOfTheFiveDestinationsAndLiesAroundItsCentre() {
        // Catches swapped lat / lng, a missing digit, or a place filed under the wrong town
        assertThat(file.places()).allSatisfy(place -> {
            Coordinate centre = DESTINATIONS.entrySet().stream()
                    .filter(destination -> place.externalId().startsWith(destination.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
            assertThat(centre).as("destination of %s", place.externalId()).isNotNull();
            assertThat(new Coordinate(place.lat(), place.lng()).distanceMetersTo(centre))
                    .as("metres from the centre for %s", place.externalId())
                    .isLessThan(MAX_DISTANCE_FROM_CENTRE_METERS);
        });
    }

    @Test
    void everyDestinationHasEnoughPlacesOfSeveralKinds() {
        DESTINATIONS.keySet().forEach(prefix -> {
            List<MockPlace> places = file.places().stream().filter(place -> place.externalId().startsWith(prefix)).toList();

            assertThat(places).as("places of %s", prefix).hasSizeGreaterThanOrEqualTo(MIN_PLACES_PER_DESTINATION);
            // Something to see, to eat, to sleep in and a way to get there: enough to plan a day offline
            assertThat(places).as("kinds of %s", prefix).extracting(MockPlace::category)
                    .contains("SIGHTSEEING", "FOOD", "ACCOMMODATION", "TRANSPORT");
        });
    }

    @Test
    void everyPlaceIsFoundByItsOwnNameTypedWithoutAccents() {
        MockMapProvider provider = new MockMapProvider(file.places());

        assertThat(file.places()).allSatisfy(place ->
                assertThat(provider.search(VietnameseText.stripAccents(place.name()), 5, null))
                        .as("search for %s", place.name())
                        .extracting(PlaceResult::externalId)
                        .contains(place.externalId()));
    }

    private static Coordinate point(String lat, String lng) {
        return new Coordinate(new BigDecimal(lat), new BigDecimal(lng));
    }

}
