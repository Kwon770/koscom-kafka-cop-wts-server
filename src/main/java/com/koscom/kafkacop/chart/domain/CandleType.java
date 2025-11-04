package com.koscom.kafkacop.chart.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

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

    /**
     * code 값으로 CandleType을 찾는다
     *
     * @param code 캔들 타입 코드 (예: "1s", "1m", "5m")
     * @return 해당하는 CandleType, 없으면 null
     */
    public static CandleType fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }
}
