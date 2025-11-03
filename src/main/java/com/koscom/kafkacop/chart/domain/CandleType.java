package com.koscom.kafkacop.chart.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 캔들 타입 Enum
 */
@Getter
@RequiredArgsConstructor
public enum CandleType {
    ONE_SECOND("1s"),
    ONE_MINUTE("1m"),
    THREE_MINUTES("3m"),
    FIVE_MINUTES("5m"),
    TEN_MINUTES("10m"),
    FIFTEEN_MINUTES("15m"),
    THIRTY_MINUTES("30m"),
    ONE_HOUR("1h"),
    FOUR_HOURS("4h"),
    ONE_DAY("1d"),
    ONE_WEEK("1w"),
    ONE_MONTH("1M");

    private final String code;
}
