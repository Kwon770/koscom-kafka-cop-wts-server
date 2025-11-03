package com.koscom.kafkacop.chart.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Candle의 복합키
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class CandleId implements Serializable {

    /**
     * 마켓ID
     */
    private Integer marketId;

    /**
     * 캔들 일시 (KST)
     */
    private LocalDateTime candleDateTime;
}
