package com.trieu.tripplanner.provider.map;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import java.math.BigDecimal;
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
        List<PlaceResult> results = provider.search("linh ung", 8, null);

        assertThat(results).extracting(PlaceResult::name).containsExactly("Chùa Linh Ứng");
        assertThat(provider.search("LINH ỨNG", 8, null)).isEqualTo(results);
        assertThat(provider.search("Linh  Ung", 8, null)).isEqualTo(results);   // two spaces
    }

    @Test
    void resultCarriesWhatTheClientNeedsToPickItLater() {
        PlaceResult result = provider.search("chùa linh ứng", 8, null).getFirst();

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
        assertThat(provider.search("son tra", 8, null)).extracting(PlaceResult::name).containsExactly("Chùa Linh Ứng");
    }

    @Test
    void everyWordOfTheKeywordMustMatchInNameOrAddress() {
        assertThat(provider.search("cho hai chau", 8, null)).extracting(PlaceResult::name)
                .containsExactly("Chợ Hàn", "Chợ Cồn");
        assertThat(provider.search("cho son tra", 8, null)).isEmpty();
    }

    @Test
    void namesStartingWithTheKeywordComeBeforeNamesContainingItAndAddressMatches() {
        // "cho": two names start with it; "da nang": names containing it come before places that only have it
        // in the address
        assertThat(provider.search("cho", 8, null)).extracting(PlaceResult::name).startsWith("Chợ Hàn", "Chợ Cồn");

        List<String> names = provider.search("da nang", 20, null).stream().map(PlaceResult::name).toList();
        assertThat(names).startsWith(
                "Bảo tàng Nghệ thuật Điêu khắc Chăm Đà Nẵng",
                "Khách sạn Novotel Đà Nẵng",
                "Sân bay quốc tế Đà Nẵng",
                "Ga Đà Nẵng");
        assertThat(names.indexOf("Chùa Linh Ứng")).isGreaterThan(names.indexOf("Ga Đà Nẵng"));
    }

    @Test
    void sameKeywordAlwaysGivesTheSameListInTheSameOrder() {
        List<PlaceResult> first = provider.search("da nang", 20, null);

        assertThat(provider.search("da nang", 20, null)).isEqualTo(first);
        // A second provider built from the same file agrees: nothing depends on the instance
        assertThat(new MockMapProvider(new ObjectMapper()).search("da nang", 20, null)).isEqualTo(first);
    }

    @Test
    void returnsAtMostLimitResultsKeepingTheBestOnes() {
        List<PlaceResult> all = provider.search("da nang", 20, null);

        assertThat(all.size()).isGreaterThan(3);
        assertThat(provider.search("da nang", 3, null)).isEqualTo(all.subList(0, 3));
    }

    @Test
    void nothingMatchesGivesAnEmptyList() {
        assertThat(provider.search("khong co noi nay", 8, null)).isEmpty();
    }

    // ---------- ordering around a reference point ----------

    /** Four markets in file order: one in Hồ Chí Minh City, three in Đà Nẵng (one only contains the keyword). */
    private final MockMapProvider twoCities = new MockMapProvider(List.of(
            place("hcm-ben-thanh", "Chợ Bến Thành", "Phường Bến Thành, TP. Hồ Chí Minh", "10.7725", "106.6980"),
            place("dn-cho-han", "Chợ Hàn", "Phường Hải Châu, Đà Nẵng", "16.0683525", "108.2242830"),
            place("dn-sieu-thi", "Siêu thị cạnh chợ Cồn", "Phường Hải Châu, Đà Nẵng", "16.0680000", "108.2146000"),
            place("dn-cho-con", "Chợ Cồn", "Phường Hải Châu, Đà Nẵng", "16.0681275", "108.2145213")));

    private static final Coordinate DA_NANG_CENTRE = point("16.0544", "108.2022");
    private static final Coordinate HO_CHI_MINH_CENTRE = point("10.7769", "106.7009");

    @Test
    void withoutAReferencePointOnlyTheNameDecidesTheOrder() {
        assertThat(twoCities.search("cho", 8, null)).extracting(PlaceResult::name)
                .containsExactly("Chợ Bến Thành", "Chợ Hàn", "Chợ Cồn", "Siêu thị cạnh chợ Cồn");
    }

    @Test
    void placesAroundTheReferencePointComeBeforePlacesElsewhere() {
        // Planning a Đà Nẵng trip: its markets first, even the weaker name match, then the other city
        assertThat(twoCities.search("cho", 8, DA_NANG_CENTRE)).extracting(PlaceResult::name)
                .containsExactly("Chợ Cồn", "Chợ Hàn", "Siêu thị cạnh chợ Cồn", "Chợ Bến Thành");

        List<String> fromHoChiMinh = twoCities.search("cho", 8, HO_CHI_MINH_CENTRE).stream().map(PlaceResult::name).toList();
        assertThat(fromHoChiMinh).startsWith("Chợ Bến Thành").endsWith("Siêu thị cạnh chợ Cồn").hasSize(4);
    }

    @Test
    void amongPlacesOfTheSameRankTheCloserOneComesFirst() {
        // Both names start with "cho"; Chợ Cồn is 2.0 km from the centre, Chợ Hàn 2.8 km
        assertThat(twoCities.search("cho", 2, DA_NANG_CENTRE)).extracting(PlaceResult::name)
                .containsExactly("Chợ Cồn", "Chợ Hàn");
    }

    @Test
    void referencePointChangesTheOrderButNeverWhatMatches() {
        Coordinate sonTra = point("16.1000", "108.2780");
        Coordinate baNa = point("15.9958", "107.9890");

        List<String> fromSonTra = provider.search("da nang", 20, sonTra).stream().map(PlaceResult::name).toList();
        List<String> fromBaNa = provider.search("da nang", 20, baNa).stream().map(PlaceResult::name).toList();

        assertThat(fromSonTra).containsExactlyInAnyOrderElementsOf(fromBaNa);
        assertThat(fromSonTra.indexOf("Chùa Linh Ứng")).isLessThan(fromSonTra.indexOf("Sun World Bà Nà Hills"));
        assertThat(fromBaNa.indexOf("Sun World Bà Nà Hills")).isLessThan(fromBaNa.indexOf("Chùa Linh Ứng"));
        // Still deterministic with a reference point
        assertThat(provider.search("da nang", 20, sonTra).stream().map(PlaceResult::name).toList()).isEqualTo(fromSonTra);
    }

    private static MockMapProvider.MockPlace place(String externalId, String name, String address, String lat, String lng) {
        return new MockMapProvider.MockPlace(externalId, name, address, new BigDecimal(lat), new BigDecimal(lng), "SHOPPING");
    }

    private static Coordinate point(String lat, String lng) {
        return new Coordinate(new BigDecimal(lat), new BigDecimal(lng));
    }

}
