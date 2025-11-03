package com.koscom.kafkacop.orderbook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Orderbook5의 복합키
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class Orderbook5Id implements Serializable {

    /**
     * 마켓ID
     */
    private Integer marketId;

    /**
     * 호가 일시 (KST)
     */
	@Column(name = "orderbook_date_time", columnDefinition = "datetime(6)")
    private LocalDateTime orderbookDateTime;
}
