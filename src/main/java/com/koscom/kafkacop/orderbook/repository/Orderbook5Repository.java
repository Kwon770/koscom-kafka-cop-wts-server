package com.koscom.kafkacop.orderbook.repository;

import com.koscom.kafkacop.orderbook.domain.Orderbook5;
import com.koscom.kafkacop.orderbook.domain.Orderbook5Id;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface Orderbook5Repository extends JpaRepository<Orderbook5, Orderbook5Id> {

    /**
     * 마켓 ID로 최신 오더북 조회 (JOIN 없이 PK 인덱스 활용)
     *
     * @param marketId 마켓 ID
     * @return 최신 오더북
     */
    @Query("""
        SELECT o FROM Orderbook5 o
        WHERE o.id.marketId = :marketId
        ORDER BY o.id.orderbookDateTime DESC
        LIMIT 1
        """)
    Optional<Orderbook5> findLatestByMarketId(@Param("marketId") Integer marketId);

    /**
     * 거래소 코드와 마켓 코드로 최신 오더북 조회
     * @deprecated 성능 이슈로 findLatestByMarketId 사용 권장
     *
     * @param exchangeCode 거래소 코드 (예: UPBIT)
     * @param code 마켓 코드 (예: KRW/BTC)
     * @return 최신 오더북
     */
    @Deprecated
    @Query("""
        SELECT o FROM Orderbook5 o JOIN o.market m JOIN m.exchange e
        WHERE e.exchangeCode = :exchangeCode AND o.code = :code
        ORDER BY o.id.orderbookDateTime DESC
        LIMIT 1
        """)
    Optional<Orderbook5> findLatestByExchangeCodeAndCode(
        @Param("exchangeCode") String exchangeCode,
        @Param("code") String code
    );

    /**
     * 마켓 코드로 최신 오더북 조회
     * @deprecated exchange 파라미터를 함께 사용해야 함
     *
     * @param code 마켓 코드 (예: KRW/BTC)
     * @return 최신 오더북
     */
    @Deprecated
    @Query("""
        SELECT o FROM Orderbook5 o
        WHERE o.code = :code
        ORDER BY o.id.orderbookDateTime DESC
        LIMIT 1
        """)
    Optional<Orderbook5> findLatestByCode(@Param("code") String code);

    @Modifying
    @Query(value = """
        INSERT INTO md_ob_top5 (
            market_id, orderbook_date_time, code,
            total_ask_size, total_bid_size,
            ask_p1, ask_q1, bid_p1, bid_q1,
            ask_p2, ask_q2, bid_p2, bid_q2,
            ask_p3, ask_q3, bid_p3, bid_q3,
            ask_p4, ask_q4, bid_p4, bid_q4,
            ask_p5, ask_q5, bid_p5, bid_q5
        ) VALUES (
            :marketId, :orderbookDateTime, :code,
            :totalAskSize, :totalBidSize,
            :askPrice1, :askQuantity1, :bidPrice1, :bidQuantity1,
            :askPrice2, :askQuantity2, :bidPrice2, :bidQuantity2,
            :askPrice3, :askQuantity3, :bidPrice3, :bidQuantity3,
            :askPrice4, :askQuantity4, :bidPrice4, :bidQuantity4,
            :askPrice5, :askQuantity5, :bidPrice5, :bidQuantity5
        )
        ON DUPLICATE KEY UPDATE
            code = VALUES(code),
            total_ask_size = VALUES(total_ask_size),
            total_bid_size = VALUES(total_bid_size),
            ask_p1 = VALUES(ask_p1), ask_q1 = VALUES(ask_q1),
            bid_p1 = VALUES(bid_p1), bid_q1 = VALUES(bid_q1),
            ask_p2 = VALUES(ask_p2), ask_q2 = VALUES(ask_q2),
            bid_p2 = VALUES(bid_p2), bid_q2 = VALUES(bid_q2),
            ask_p3 = VALUES(ask_p3), ask_q3 = VALUES(ask_q3),
            bid_p3 = VALUES(bid_p3), bid_q3 = VALUES(bid_q3),
            ask_p4 = VALUES(ask_p4), ask_q4 = VALUES(ask_q4),
            bid_p4 = VALUES(bid_p4), bid_q4 = VALUES(bid_q4),
            ask_p5 = VALUES(ask_p5), ask_q5 = VALUES(ask_q5),
            bid_p5 = VALUES(bid_p5), bid_q5 = VALUES(bid_q5)
        """, nativeQuery = true)
    void upsert(
        @Param("marketId") Integer marketId,
        @Param("orderbookDateTime") LocalDateTime orderbookDateTime,
        @Param("code") String code,
        @Param("totalAskSize") BigDecimal totalAskSize,
        @Param("totalBidSize") BigDecimal totalBidSize,
        @Param("askPrice1") BigDecimal askPrice1,
        @Param("askQuantity1") BigDecimal askQuantity1,
        @Param("bidPrice1") BigDecimal bidPrice1,
        @Param("bidQuantity1") BigDecimal bidQuantity1,
        @Param("askPrice2") BigDecimal askPrice2,
        @Param("askQuantity2") BigDecimal askQuantity2,
        @Param("bidPrice2") BigDecimal bidPrice2,
        @Param("bidQuantity2") BigDecimal bidQuantity2,
        @Param("askPrice3") BigDecimal askPrice3,
        @Param("askQuantity3") BigDecimal askQuantity3,
        @Param("bidPrice3") BigDecimal bidPrice3,
        @Param("bidQuantity3") BigDecimal bidQuantity3,
        @Param("askPrice4") BigDecimal askPrice4,
        @Param("askQuantity4") BigDecimal askQuantity4,
        @Param("bidPrice4") BigDecimal bidPrice4,
        @Param("bidQuantity4") BigDecimal bidQuantity4,
        @Param("askPrice5") BigDecimal askPrice5,
        @Param("askQuantity5") BigDecimal askQuantity5,
        @Param("bidPrice5") BigDecimal bidPrice5,
        @Param("bidQuantity5") BigDecimal bidQuantity5
    );
}
