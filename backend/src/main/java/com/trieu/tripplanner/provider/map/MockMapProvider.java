package com.trieu.tripplanner.provider.map;

import com.trieu.tripplanner.common.util.VietnameseText;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
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
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.providers.map", havingValue = "mock", matchIfMissing = true)
public class MockMapProvider implements MapProvider {

    static final String DATA_FILE = "mock/places.json";

    private final List<IndexedPlace> places;

    public MockMapProvider(ObjectMapper objectMapper) {
        this.places = load(objectMapper).stream().map(IndexedPlace::of).toList();
        log.info("[mock-map] loaded {} places from {}", places.size(), DATA_FILE);
    }

    @Override
    public List<PlaceResult> search(String query, int limit) {
        String keyword = normalize(query);
        List<String> words = Arrays.asList(keyword.split(" "));
        return places.stream()
                .filter(place -> place.matchesAll(words))
                // The sort is stable: places of the same rank stay in file order
                .sorted(Comparator.comparingInt(place -> place.rank(keyword)))
                .limit(limit)
                .map(IndexedPlace::result)
                .toList();
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

    }

}
