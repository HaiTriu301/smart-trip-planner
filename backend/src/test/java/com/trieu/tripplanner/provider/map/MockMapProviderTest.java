package com.trieu.tripplanner.provider.map;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * Runs against the real mock/places.json, no Spring context: the provider only needs a JSON mapper.
 */
class MockMapProviderTest {

    private final MockMapProvider provider = new MockMapProvider(new ObjectMapper());

    @Test
    void findsAPlaceTypedWithoutAccentsOrCapitals() {
        List<PlaceResult> results = provider.search("linh ung", 8);

        assertThat(results).extracting(PlaceResult::name).containsExactly("Chùa Linh Ứng");
        assertThat(provider.search("LINH ỨNG", 8)).isEqualTo(results);
        assertThat(provider.search("Linh  Ung", 8)).isEqualTo(results);   // two spaces
    }

    @Test
    void resultCarriesWhatTheClientNeedsToPickItLater() {
        PlaceResult result = provider.search("chùa linh ứng", 8).getFirst();

        assertThat(result.provider()).isEqualTo(PlaceProvider.MOCK);
        assertThat(result.externalId()).isEqualTo("da-nang-chua-linh-ung");
        assertThat(result.address()).isEqualTo("Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng");
        assertThat(result.lat()).isEqualByComparingTo("16.1001567");
        assertThat(result.lng()).isEqualByComparingTo("108.2784112");
        assertThat(result.category()).isEqualTo("SIGHTSEEING");
    }

    @Test
    void matchesTheAddressToo() {
        // "Sơn Trà" is in no place name, only in an address
        assertThat(provider.search("son tra", 8)).extracting(PlaceResult::name).containsExactly("Chùa Linh Ứng");
    }

    @Test
    void everyWordOfTheKeywordMustMatchInNameOrAddress() {
        assertThat(provider.search("cho hai chau", 8)).extracting(PlaceResult::name)
                .containsExactly("Chợ Hàn", "Chợ Cồn");
        assertThat(provider.search("cho son tra", 8)).isEmpty();
    }

    @Test
    void namesStartingWithTheKeywordComeBeforeNamesContainingItAndAddressMatches() {
        // "cho": two names start with it; "da nang": names containing it come before places that only have it
        // in the address
        assertThat(provider.search("cho", 8)).extracting(PlaceResult::name).startsWith("Chợ Hàn", "Chợ Cồn");

        List<String> names = provider.search("da nang", 20).stream().map(PlaceResult::name).toList();
        assertThat(names).startsWith(
                "Bảo tàng Nghệ thuật Điêu khắc Chăm Đà Nẵng",
                "Khách sạn Novotel Đà Nẵng",
                "Sân bay quốc tế Đà Nẵng",
                "Ga Đà Nẵng");
        assertThat(names.indexOf("Chùa Linh Ứng")).isGreaterThan(names.indexOf("Ga Đà Nẵng"));
    }

    @Test
    void sameKeywordAlwaysGivesTheSameListInTheSameOrder() {
        List<PlaceResult> first = provider.search("da nang", 20);

        assertThat(provider.search("da nang", 20)).isEqualTo(first);
        // A second provider built from the same file agrees: nothing depends on the instance
        assertThat(new MockMapProvider(new ObjectMapper()).search("da nang", 20)).isEqualTo(first);
    }

    @Test
    void returnsAtMostLimitResultsKeepingTheBestOnes() {
        List<PlaceResult> all = provider.search("da nang", 20);

        assertThat(all.size()).isGreaterThan(3);
        assertThat(provider.search("da nang", 3)).isEqualTo(all.subList(0, 3));
    }

    @Test
    void nothingMatchesGivesAnEmptyList() {
        assertThat(provider.search("khong co noi nay", 8)).isEmpty();
    }

}
