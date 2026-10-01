package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.dto.request.SavePlaceRequest;
import com.trieu.tripplanner.dto.response.PlaceResponse;
import com.trieu.tripplanner.dto.response.PlaceResultResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.FieldViolation;
import com.trieu.tripplanner.exception.ResourceNotFoundException;
import com.trieu.tripplanner.mapper.PlaceMapper;
import com.trieu.tripplanner.model.Place;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import com.trieu.tripplanner.repository.PlaceRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The map source is a mock; the MapStruct mapper is the real generated one.
 */
@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    private static final PlaceResult LINH_UNG = new PlaceResult(PlaceProvider.MOCK, "da-nang-chua-linh-ung",
            "Chùa Linh Ứng", "Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng", new BigDecimal("16.1001567"),
            new BigDecimal("108.2784112"), "SIGHTSEEING");

    @Mock
    private MapProvider mapProvider;

    @Mock
    private PlaceRepository placeRepository;

    private PlaceService placeService;

    @BeforeEach
    void setUp() {
        placeService = new PlaceService(mapProvider, placeRepository, Mappers.getMapper(PlaceMapper.class));
    }

    @Test
    void searchTrimsTheKeywordAndReturnsEveryFieldOfTheResultsInTheSourceOrder() {
        when(mapProvider.search("linh ung", 8, null)).thenReturn(List.of(
                new PlaceResult(PlaceProvider.MOCK, "da-nang-chua-linh-ung", "Chùa Linh Ứng",
                        "Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng", new BigDecimal("16.1001567"),
                        new BigDecimal("108.2784112"), "SIGHTSEEING"),
                new PlaceResult(PlaceProvider.MOCK, "x-2", "Linh Ứng Bãi Bụt", null, new BigDecimal("16.1"),
                        new BigDecimal("108.2"), null)));

        List<PlaceResultResponse> results = placeService.search("  linh ung ", 8, null, null);

        assertThat(results).containsExactly(
                new PlaceResultResponse(PlaceProvider.MOCK, "da-nang-chua-linh-ung", "Chùa Linh Ứng",
                        "Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng", new BigDecimal("16.1001567"),
                        new BigDecimal("108.2784112"), "SIGHTSEEING"),
                new PlaceResultResponse(PlaceProvider.MOCK, "x-2", "Linh Ứng Bãi Bụt", null, new BigDecimal("16.1"),
                        new BigDecimal("108.2"), null));
    }

    @Test
    void searchWithNoMatchReturnsAnEmptyList() {
        when(mapProvider.search("khong co", 8, null)).thenReturn(List.of());

        assertThat(placeService.search("khong co", 8, null, null)).isEmpty();
    }

    @Test
    void searchPassesTheReferencePointWhenBothNumbersAreSent() {
        BigDecimal lat = new BigDecimal("16.0544");
        BigDecimal lng = new BigDecimal("108.2022");
        when(mapProvider.search("cho", 8, new Coordinate(lat, lng))).thenReturn(List.of());

        placeService.search("cho", 8, lat, lng);

        verify(mapProvider).search("cho", 8, new Coordinate(lat, lng));
    }

    @Test
    void halfACoordinateIsAValidationErrorOnTheMissingNumber() {
        assertThatThrownBy(() -> placeService.search("cho", 8, new BigDecimal("16.05"), null))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException rule = (BusinessRuleException) ex;
                    assertThat(rule.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(rule.getDetails()).extracting(FieldViolation::field).containsExactly("lng");
                });
        assertThatThrownBy(() -> placeService.search("cho", 8, null, new BigDecimal("108.2")))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getDetails())
                        .extracting(FieldViolation::field).containsExactly("lat"));

        verifyNoInteractions(mapProvider);
    }

    // ---------- getOrCreate ----------

    @Test
    void firstPickStoresACopyBuiltFromTheSourceNotFromTheRequest() {
        when(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "da-nang-chua-linh-ung"))
                .thenReturn(Optional.empty());
        when(mapProvider.lookup("da-nang-chua-linh-ung")).thenReturn(Optional.of(LINH_UNG));
        when(placeRepository.saveAndFlush(any(Place.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 31L));

        PlaceResponse response = placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "da-nang-chua-linh-ung"));

        ArgumentCaptor<Place> stored = ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).saveAndFlush(stored.capture());
        // The request carries two values only; every fact of the place comes from the lookup
        assertThat(stored.getValue().getProvider()).isEqualTo(PlaceProvider.MOCK);
        assertThat(stored.getValue().getExternalId()).isEqualTo("da-nang-chua-linh-ung");
        assertThat(stored.getValue().getName()).isEqualTo("Chùa Linh Ứng");
        assertThat(stored.getValue().getAddress()).isEqualTo("Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng");
        assertThat(stored.getValue().getLat()).isEqualByComparingTo("16.1001567");
        assertThat(stored.getValue().getLng()).isEqualByComparingTo("108.2784112");
        assertThat(stored.getValue().getCategory()).isEqualTo("SIGHTSEEING");
        assertThat(response).isEqualTo(new PlaceResponse(31L, PlaceProvider.MOCK, "Chùa Linh Ứng",
                "Đường Hoàng Sa, Phường Sơn Trà, Đà Nẵng", new BigDecimal("16.1001567"), new BigDecimal("108.2784112"),
                "SIGHTSEEING"));
    }

    @Test
    void secondPickReturnsTheStoredCopyWithoutAskingTheSourceOrWritingAgain() {
        when(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "da-nang-chua-linh-ung"))
                .thenReturn(Optional.of(storedLinhUng(31L)));

        PlaceResponse response = placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "da-nang-chua-linh-ung"));

        assertThat(response.id()).isEqualTo(31L);
        verify(placeRepository, never()).saveAndFlush(any());
        verifyNoInteractions(mapProvider);
    }

    @Test
    void externalIdIsTrimmedBeforeItIsLookedUp() {
        when(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "da-nang-chua-linh-ung"))
                .thenReturn(Optional.of(storedLinhUng(31L)));

        assertThat(placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "  da-nang-chua-linh-ung ")).id())
                .isEqualTo(31L);
    }

    @Test
    void placeTheSourceDoesNotKnowIsNotFoundAndNothingIsStored() {
        when(placeRepository.findByProviderAndExternalId(PlaceProvider.MOCK, "bia-ra")).thenReturn(Optional.empty());
        when(mapProvider.lookup("bia-ra")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> placeService.getOrCreate(new SavePlaceRequest(PlaceProvider.MOCK, "bia-ra")))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        verify(placeRepository, never()).saveAndFlush(any());
    }

    private static Place storedLinhUng(Long id) {
        return withId(Place.builder()
                .provider(LINH_UNG.provider())
                .externalId(LINH_UNG.externalId())
                .name(LINH_UNG.name())
                .address(LINH_UNG.address())
                .lat(LINH_UNG.lat())
                .lng(LINH_UNG.lng())
                .category(LINH_UNG.category())
                .build(), id);
    }

    /** Sets the id the database would assign; BaseEntity deliberately has no setter for it. */
    private static Place withId(Place place, Long id) {
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }

}
