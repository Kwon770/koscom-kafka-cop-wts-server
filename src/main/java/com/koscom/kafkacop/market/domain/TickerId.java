package com.koscom.kafkacop.market.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Ticker 복합키 클래스
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TickerId implements Serializable {

    private Integer marketId;
    private LocalDateTime sourceCreatedAt;
}