package com.koscom.kafkacop.market.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 거래소 기준 테이블
 */
@Entity
@Table(
    name = "ref_exchange",
    indexes = {
        @Index(name = "ix_exchange_code", columnList = "exchange_code")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exchange {

    /**
     * 거래소ID (PK, Auto Increment)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exchange_id", nullable = false)
    private Integer exchangeId;

    /**
     * 거래소코드 (UNIQUE)
     */
    @Column(name = "exchange_code", length = 30, unique = true)
    private String exchangeCode;

    @Builder
    public Exchange(String exchangeCode) {
        this.exchangeCode = exchangeCode;
    }
}
