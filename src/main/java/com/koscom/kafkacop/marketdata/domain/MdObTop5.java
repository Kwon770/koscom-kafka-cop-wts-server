package com.koscom.kafkacop.marketdata.domain;

import com.koscom.kafkacop.reference.domain.RefMarket;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 5호가 스냅샷 테이블 (ASK/BID 1~5)
 */
@Entity
@Table(
    name = "md_ob_top5",
    indexes = {
        @Index(name = "ix_ob5_code", columnList = "code"),
        @Index(name = "ix_ob5_latest", columnList = "market_id, timestamp")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MdObTop5 {

    /**
     * 복합 PK (market_id, timestamp)
     */
    @EmbeddedId
    private MdObTop5Id id;

    /**
     * 마켓 참조 (FK)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("marketId")
    @JoinColumn(name = "market_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ob_top5_market"))
    private RefMarket market;

    /**
     * 마켓 코드
     */
    @Column(name = "code", length = 20)
    private String code;

    /**
     * 총 매도 잔량
     */
    @Column(name = "total_ask_size", precision = 38, scale = 18)
    private BigDecimal totalAskSize;

    /**
     * 총 매수 잔량
     */
    @Column(name = "total_bid_size", precision = 38, scale = 18)
    private BigDecimal totalBidSize;

    // ASK (매도) 1~5호가
    @Column(name = "ask_p1", precision = 28, scale = 8)
    private BigDecimal askPrice1;

    @Column(name = "ask_q1", precision = 38, scale = 18)
    private BigDecimal askQuantity1;

    @Column(name = "ask_p2", precision = 28, scale = 8)
    private BigDecimal askPrice2;

    @Column(name = "ask_q2", precision = 38, scale = 18)
    private BigDecimal askQuantity2;

    @Column(name = "ask_p3", precision = 28, scale = 8)
    private BigDecimal askPrice3;

    @Column(name = "ask_q3", precision = 38, scale = 18)
    private BigDecimal askQuantity3;

    @Column(name = "ask_p4", precision = 28, scale = 8)
    private BigDecimal askPrice4;

    @Column(name = "ask_q4", precision = 38, scale = 18)
    private BigDecimal askQuantity4;

    @Column(name = "ask_p5", precision = 28, scale = 8)
    private BigDecimal askPrice5;

    @Column(name = "ask_q5", precision = 38, scale = 18)
    private BigDecimal askQuantity5;

    // BID (매수) 1~5호가
    @Column(name = "bid_p1", precision = 28, scale = 8)
    private BigDecimal bidPrice1;

    @Column(name = "bid_q1", precision = 38, scale = 18)
    private BigDecimal bidQuantity1;

    @Column(name = "bid_p2", precision = 28, scale = 8)
    private BigDecimal bidPrice2;

    @Column(name = "bid_q2", precision = 38, scale = 18)
    private BigDecimal bidQuantity2;

    @Column(name = "bid_p3", precision = 28, scale = 8)
    private BigDecimal bidPrice3;

    @Column(name = "bid_q3", precision = 38, scale = 18)
    private BigDecimal bidQuantity3;

    @Column(name = "bid_p4", precision = 28, scale = 8)
    private BigDecimal bidPrice4;

    @Column(name = "bid_q4", precision = 38, scale = 18)
    private BigDecimal bidQuantity4;

    @Column(name = "bid_p5", precision = 28, scale = 8)
    private BigDecimal bidPrice5;

    @Column(name = "bid_q5", precision = 38, scale = 18)
    private BigDecimal bidQuantity5;

    @Builder
    public MdObTop5(RefMarket market, Instant timestamp, String code,
                    BigDecimal totalAskSize, BigDecimal totalBidSize,
                    BigDecimal askPrice1, BigDecimal askQuantity1,
                    BigDecimal askPrice2, BigDecimal askQuantity2,
                    BigDecimal askPrice3, BigDecimal askQuantity3,
                    BigDecimal askPrice4, BigDecimal askQuantity4,
                    BigDecimal askPrice5, BigDecimal askQuantity5,
                    BigDecimal bidPrice1, BigDecimal bidQuantity1,
                    BigDecimal bidPrice2, BigDecimal bidQuantity2,
                    BigDecimal bidPrice3, BigDecimal bidQuantity3,
                    BigDecimal bidPrice4, BigDecimal bidQuantity4,
                    BigDecimal bidPrice5, BigDecimal bidQuantity5) {
        this.id = new MdObTop5Id(
            market != null ? market.getMarketId() : null,
            timestamp
        );
        this.market = market;
        this.code = code;
        this.totalAskSize = totalAskSize;
        this.totalBidSize = totalBidSize;
        this.askPrice1 = askPrice1;
        this.askQuantity1 = askQuantity1;
        this.askPrice2 = askPrice2;
        this.askQuantity2 = askQuantity2;
        this.askPrice3 = askPrice3;
        this.askQuantity3 = askQuantity3;
        this.askPrice4 = askPrice4;
        this.askQuantity4 = askQuantity4;
        this.askPrice5 = askPrice5;
        this.askQuantity5 = askQuantity5;
        this.bidPrice1 = bidPrice1;
        this.bidQuantity1 = bidQuantity1;
        this.bidPrice2 = bidPrice2;
        this.bidQuantity2 = bidQuantity2;
        this.bidPrice3 = bidPrice3;
        this.bidQuantity3 = bidQuantity3;
        this.bidPrice4 = bidPrice4;
        this.bidQuantity4 = bidQuantity4;
        this.bidPrice5 = bidPrice5;
        this.bidQuantity5 = bidQuantity5;
    }
}
