package com.koscom.kafkacop.util;

import java.util.Collection;
import java.util.function.Function;

import io.swagger.v3.oas.annotations.media.Schema;

public record PaginationResponse<T>(
        @Schema(description = "가져온 현재 페이지", example = "1")
        int currentPage,

        @Schema(description = "전체 페이지 수", example = "5")
        int totalPage,

        @Schema(description = "페이지당 아이템 수", example = "10")
        int itemPerPage,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "가져온 결과 목록")
        Collection<T> results
) {
    public static <T> PaginationResponse<T> of(PaginationDto<T> dto, int currentPage, int itemPerPage) {
        return new PaginationResponse<>(currentPage, dto.totalPage(), itemPerPage, dto.totalPage() > currentPage, dto.results());
    }

    public <R> PaginationResponse<R> map(Function<T, R> mappingFunction) {
        Collection<R> mappedResults = results.stream().map(mappingFunction).toList();
        return new PaginationResponse<>(currentPage, totalPage, itemPerPage, hasNext, mappedResults);
    }
}
