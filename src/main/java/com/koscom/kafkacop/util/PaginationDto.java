package com.koscom.kafkacop.util;

import java.util.Collection;

import org.springframework.data.domain.Page;

public record PaginationDto<T>(
        int totalPage,
        Collection<T> results
) {
    public static <T> PaginationDto<T> of(Page<T> page) {
        return new PaginationDto<>(page.getTotalPages(), page.getContent());
    }

    public static <T> PaginationDto<T> of(int totalPage, Collection<T> results) {
        return new PaginationDto<>(totalPage, results);
    }
}
