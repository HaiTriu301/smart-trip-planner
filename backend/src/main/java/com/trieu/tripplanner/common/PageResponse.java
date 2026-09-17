package com.trieu.tripplanner.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated payload placed inside {@link ApiResponse#data()} (design.md 10.1).
 * Keeps the JSON shape stable instead of serializing Spring Data's {@link Page} directly.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext) {

    /**
     * Map entities to DTOs before calling, e.g. {@code PageResponse.from(page.map(mapper::toResponse))}.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }

}
