package com.koscom.kafkacop.marketdata.domain;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * MdObTop5의 복합키
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class MdObTop5Id implements Serializable {

    /**
     * 마켓ID
     */
    private Integer marketId;

    /**
     * 타임스탬프 (밀리초 단위)
     */
    private Instant timestamp;
}
