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
        @UniqueConstraint(name = "ux_market", columnNames = {"exchange_id", "market_code"})
    },
    indexes = {
        @Index(name = "ix_mkt_code", columnList = "market_code")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefMarket {

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
    private RefExchange exchange;

    /**
     * 마켓코드 (예: KRW-BTC, KRW-ETH, KRW-XRP)
     */
    @Column(name = "market_code", length = 20)
    private String marketCode;

    @Builder
    public RefMarket(RefExchange exchange, String marketCode) {
        this.exchange = exchange;
        this.marketCode = marketCode;
    }
}
