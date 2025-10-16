package com.koscom.kafkacop.marketdata.domain;

import com.koscom.kafkacop.reference.domain.RefMarket;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 1초 캔들 테이블
 */
@Entity
@Table(
    name = "md_candle_sec",
    indexes = {
        @Index(name = "ix_csec_code_kst", columnList = "code, candle_date_time_kst")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MdCandleSec {

    /**
     * 복합 PK (market_id, candle_date_time_kst)
     */
    @EmbeddedId
    private MdCandleSecId id;

    /**
     * 마켓 참조 (FK)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("marketId")
    @JoinColumn(name = "market_id", nullable = false, foreignKey = @ForeignKey(name = "fk_candle_sec_market"))
    private RefMarket market;

    /**
     * 마켓 코드
     */
    @Column(name = "code", length = 20)
    private String code;

    /**
     * 타입 (예: candle.1s)
     */
    @Column(name = "type", length = 20, columnDefinition = "varchar(20) default 'candle.1s'")
    private String type;

    /**
     * 시가
     */
    @Column(name = "opening_price", precision = 28, scale = 8)
    private BigDecimal openingPrice;

    /**
     * 고가
     */
    @Column(name = "high_price", precision = 28, scale = 8)
    private BigDecimal highPrice;

    /**
     * 저가
     */
    @Column(name = "low_price", precision = 28, scale = 8)
    private BigDecimal lowPrice;

    /**
     * 종가 (현재가)
     */
    @Column(name = "trade_price", precision = 28, scale = 8)
    private BigDecimal tradePrice;

    /**
     * 캔들 누적 거래량
     */
    @Column(name = "candle_acc_trade_volume", precision = 38, scale = 18, columnDefinition = "decimal(38,18) default 0")
    private BigDecimal candleAccTradeVolume;

    /**
     * 캔들 누적 거래대금
     */
    @Column(name = "candle_acc_trade_price", precision = 38, scale = 8, columnDefinition = "decimal(38,8) default 0")
    private BigDecimal candleAccTradePrice;

    @Builder
    public MdCandleSec(RefMarket market, LocalDateTime candleDateTimeKst, String code,
                       String type, BigDecimal openingPrice, BigDecimal highPrice,
                       BigDecimal lowPrice, BigDecimal tradePrice,
                       BigDecimal candleAccTradeVolume, BigDecimal candleAccTradePrice) {
        this.id = new MdCandleSecId(
            market != null ? market.getMarketId() : null,
            candleDateTimeKst
        );
        this.market = market;
        this.code = code;
        this.type = type != null ? type : "candle.1s";
        this.openingPrice = openingPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.tradePrice = tradePrice;
        this.candleAccTradeVolume = candleAccTradeVolume != null ? candleAccTradeVolume : BigDecimal.ZERO;
        this.candleAccTradePrice = candleAccTradePrice != null ? candleAccTradePrice : BigDecimal.ZERO;
    }
}
