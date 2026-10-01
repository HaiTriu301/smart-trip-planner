package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.trieu.tripplanner.common.constant.ErrorCode;
import com.trieu.tripplanner.dto.response.PlaceResultResponse;
import com.trieu.tripplanner.exception.BusinessRuleException;
import com.trieu.tripplanner.exception.FieldViolation;
import com.trieu.tripplanner.mapper.PlaceMapper;
import com.trieu.tripplanner.model.enums.PlaceProvider;
import com.trieu.tripplanner.provider.map.MapProvider;
import com.trieu.tripplanner.provider.map.dto.Coordinate;
import com.trieu.tripplanner.provider.map.dto.PlaceResult;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The map source is a mock; the MapStruct mapper is the real generated one.
 */
@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private MapProvider mapProvider;

    private PlaceService placeService;

    @BeforeEach
    void setUp() {
        placeService = new PlaceService(mapProvider, Mappers.getMapper(PlaceMapper.class));
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

}
