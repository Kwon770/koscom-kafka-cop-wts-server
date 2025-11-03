package com.koscom.kafkacop.market.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 거래소별 마켓 테이블
 */
@Entity
@Table(
    name = "ref_market",
    uniqueConstraints = {
        // (exchange_id, market_code) 복합 유니크 제약 조건
        // 이 제약은 자동으로 복합 인덱스를 생성하므로 JOIN + WHERE 최적화에 활용됨
        @UniqueConstraint(name = "ux_market", columnNames = {"exchange_id", "market_code"})
    },
    indexes = {
        // market_code 단독 조회 최적화 (배치 조회 시 IN 조건)
        @Index(name = "ix_mkt_code", columnList = "market_code")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Market {

    /**
     * 마켓ID (PK, Auto Increment)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "market_id", nullable = false)
    private Integer marketId;

    /**
     * 거래소 (FK)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", nullable = false, foreignKey = @ForeignKey(name = "fk_market_exchange"))
    private Exchange exchange;

    /**
     * 마켓코드 (예: KRW-BTC, KRW-ETH, KRW-XRP)
     */
    @Column(name = "market_code", length = 20)
    private String marketCode;

    @Builder
    public Market(Exchange exchange, String marketCode) {
        this.exchange = exchange;
        this.marketCode = marketCode;
    }
}
