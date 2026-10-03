package com.trieu.tripplanner.provider.map;

import com.trieu.tripplanner.common.util.VietnameseText;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import com.trieu.tripplanner.provider.map.dto.RouteLeg;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Default map source: no network, no API key. Searches the places bundled in {@code mock/places.json}, so tests
 * and an offline machine always get the same answer (CLAUDE.md rules 20 and 24).
 * <p>
 * Matching ignores case and Vietnamese accents: "linh ung" finds "Chùa Linh Ứng". Every word of the keyword
 * must appear in the name or the address. Order: names that start with the keyword, then names that contain
 * it, then the rest; inside each group the order of the file is kept, which makes the result deterministic.
 * <p>
 * With a reference point, places around it (within {@link #NEAR_RADIUS_METERS}) come first whatever their
 * rank, so a search made while planning a Đà Nẵng trip shows Đà Nẵng before other cities. Inside "around" and
 * inside "elsewhere" the order is: rank by name as above, then the closer place first.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.providers.map", havingValue = "mock", matchIfMissing = true)
public class MockMapProvider implements MapProvider {

    static final String DATA_FILE = "mock/places.json";

    /** Roughly "the same city and its surroundings": Bà Nà Hills is 25 km from the centre of Đà Nẵng. */
    static final double NEAR_RADIUS_METERS = 50_000;

    /** Roads are never straight: a way is taken to be this much longer than the straight line (design.md 7.2). */
    static final double ROAD_FACTOR = 1.3;

    /** One made-up means of travel, about a motorbike or a car in town (design.md 7.2). */
    static final double SPEED_KM_PER_HOUR = 30;

    private final List<IndexedPlace> places;
    private final Map<String, PlaceResult> byExternalId;

    // Two constructors: Spring must be told which one builds the bean
    @Autowired
    public MockMapProvider(ObjectMapper objectMapper) {
        this(load(objectMapper));
        log.info("[mock-map] loaded {} places from {}", places.size(), DATA_FILE);
    }

    /** Tests pass their own places to check the ordering rules without depending on the bundled file. */
    MockMapProvider(List<MockPlace> places) {
        this.places = places.stream().map(IndexedPlace::of).toList();
        this.byExternalId = this.places.stream()
                .map(IndexedPlace::result)
                .collect(Collectors.toUnmodifiableMap(PlaceResult::externalId, Function.identity()));
    }

    @Override
    public PlaceProvider provider() {
        return PlaceProvider.MOCK;
    }

    @Override
    public List<PlaceResult> search(String query, int limit, Coordinate near) {
        String keyword = normalize(query);
        List<String> words = Arrays.asList(keyword.split(" "));
        return places.stream()
                .filter(place -> place.matchesAll(words))
                // The sort is stable: places equal on every criterion stay in file order
                .sorted(order(keyword, near))
                .limit(limit)
                .map(IndexedPlace::result)
                .toList();
    }

    @Override
    public Optional<PlaceResult> lookup(String externalId) {
        return Optional.ofNullable(byExternalId.get(externalId));
    }

    /**
     * No road network here: each leg is the straight line between its two points made {@link #ROAD_FACTOR}
     * times longer, travelled at {@link #SPEED_KM_PER_HOUR}. The time is computed from the rounded distance,
     * so the two numbers of a leg always agree when checked by hand.
     */
    @Override
    public List<RouteLeg> route(List<Coordinate> points) {
        List<RouteLeg> legs = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            long distanceMeters = Math.round(points.get(i - 1).distanceMetersTo(points.get(i)) * ROAD_FACTOR);
            long durationSeconds = Math.round(distanceMeters * 3600 / (SPEED_KM_PER_HOUR * 1000));
            legs.add(new RouteLeg(distanceMeters, durationSeconds));
        }
        return List.copyOf(legs);
    }

    private static Comparator<IndexedPlace> order(String keyword, Coordinate near) {
        Comparator<IndexedPlace> byName = Comparator.comparingInt(place -> place.rank(keyword));
        if (near == null) {
            return byName;
        }
        return Comparator.<IndexedPlace>comparingInt(place -> place.distanceTo(near) <= NEAR_RADIUS_METERS ? 0 : 1)
                .thenComparing(byName)
                .thenComparingDouble(place -> place.distanceTo(near));
    }

    /** Lower case, no accents, single spaces: the form in which keyword and place text are compared. */
    static String normalize(String text) {
        return VietnameseText.stripAccents(text).toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private static List<MockPlace> load(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(DATA_FILE).getInputStream()) {
            return objectMapper.readValue(in, MockPlaceFile.class).places();
        }
        catch (IOException ex) {
            // The file ships inside the jar: failing here means a broken build, so stop the startup
            throw new UncheckedIOException("Cannot read " + DATA_FILE, ex);
        }
    }

    /** The JSON document: where the data comes from (licence notice) and the places themselves. */
    record MockPlaceFile(String source, List<MockPlace> places) {
    }

    record MockPlace(String externalId, String name, String address, BigDecimal lat, BigDecimal lng, String category) {
    }

    /** A place with its searchable text prepared once at startup instead of on every request. */
    private record IndexedPlace(PlaceResult result, String name, String nameAndAddress) {

        static IndexedPlace of(MockPlace place) {
            PlaceResult result = new PlaceResult(PlaceProvider.MOCK, place.externalId(), place.name(), place.address(),
                    place.lat(), place.lng(), place.category());
            String name = normalize(place.name());
            String address = (place.address() == null) ? "" : normalize(place.address());
            return new IndexedPlace(result, name, name + " " + address);
        }

        boolean matchesAll(List<String> words) {
            return words.stream().allMatch(nameAndAddress::contains);
        }

        /** 0: the name starts with the keyword · 1: the name contains it · 2: matched through the address or by separate words */
        int rank(String keyword) {
            if (name.startsWith(keyword)) {
                return 0;
            }
            return name.contains(keyword) ? 1 : 2;
        }

        double distanceTo(Coordinate point) {
            return new Coordinate(result.lat(), result.lng()).distanceMetersTo(point);
        }

    }

}
