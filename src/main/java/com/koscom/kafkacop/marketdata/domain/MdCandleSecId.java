package com.koscom.kafkacop.marketdata.domain;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MdCandleSec의 복합키
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class MdCandleSecId implements Serializable {

    /**
     * 마켓ID
     */
    private Integer marketId;

    /**
     * 캔들 일시 (KST)
     */
    private LocalDateTime candleDateTimeKst;
}
