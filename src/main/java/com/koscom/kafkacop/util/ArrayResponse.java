package com.koscom.kafkacop.util;

import java.util.List;

public record ArrayResponse<T>(
    List<T> results
) {
    public static <T> ArrayResponse<T> of(List<T> results) {
        return new ArrayResponse<>(results);
    }
}
