package com.koscom.kafkacop.market.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 티커 최신 상태 테이블 (마켓당 1행)
 */
@Entity
@Table(
    name = "md_ticker_latest",
    indexes = {
        @Index(name = "ix_ticker_code", columnList = "code"),
        @Index(name = "ix_ticker_updated", columnList = "updated_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MdTickerLatest {

    /**
     * 마켓ID (PK + FK)
     */
    @Id
    @Column(name = "market_id", nullable = false)
    private Integer marketId;

    /**
     * 마켓 참조 (1:1)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "market_id", foreignKey = @ForeignKey(name = "fk_ticker_market"))
    private RefMarket market;

    /**
     * 원본 코드 (예: KRW-BTC)
     */
    @Column(name = "code", length = 20)
    private String code;

    /**
     * 현재가
     */
    @Column(name = "trade_price", precision = 28, scale = 8)
    private BigDecimal tradePrice;

    /**
     * 부호가 있는 변화율
     */
    @Column(name = "signed_change_rate", precision = 18, scale = 10)
    private BigDecimal signedChangeRate;

    /**
     * 부호가 있는 변화 금액
     */
    @Column(name = "signed_change_price", precision = 28, scale = 8)
    private BigDecimal signedChangePrice;

    /**
     * 누적 거래대금
     */
    @Column(name = "acc_trade_price", precision = 38, scale = 8)
    private BigDecimal accTradePrice;

    /**
     * 24시간 누적 거래대금
     */
    @Column(name = "24h_acc_trade_price_24h", precision = 38, scale = 8)
    private BigDecimal accTradePrice24h;

    /**
     * 업데이트 시각 (자동 갱신)
     */
    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "DATETIME(3)")
    private LocalDateTime updatedAt;

    @Builder
    public MdTickerLatest(RefMarket market, String code, BigDecimal tradePrice,
                          BigDecimal signedChangeRate, BigDecimal signedChangePrice,
                          BigDecimal accTradePrice, BigDecimal accTradePrice24h) {
        this.market = market;
        this.marketId = market != null ? market.getMarketId() : null;
        this.code = code;
        this.tradePrice = tradePrice;
        this.signedChangeRate = signedChangeRate;
        this.signedChangePrice = signedChangePrice;
        this.accTradePrice = accTradePrice;
        this.accTradePrice24h = accTradePrice24h;
    }

    public void updateTicker(BigDecimal tradePrice, BigDecimal signedChangeRate,
                            BigDecimal signedChangePrice, BigDecimal accTradePrice,
                            BigDecimal accTradePrice24h) {
        this.tradePrice = tradePrice;
        this.signedChangeRate = signedChangeRate;
        this.signedChangePrice = signedChangePrice;
        this.accTradePrice = accTradePrice;
        this.accTradePrice24h = accTradePrice24h;
    }
}
