package com.koscom.kafkacop.market.repository;

import com.koscom.kafkacop.market.domain.Ticker;
import com.koscom.kafkacop.market.domain.TickerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TickerRepository extends JpaRepository<Ticker, TickerId> {

    /**
     * 마켓ID 리스트로 각 마켓의 최신 Ticker 조회
     *
     * @param marketIds 마켓ID 리스트
     * @return 각 마켓의 최신 Ticker 리스트
     */
    @Query("""
        SELECT t FROM Ticker t
        WHERE (t.marketId, t.sourceCreatedAt) IN (
            SELECT t2.marketId, MAX(t2.sourceCreatedAt)
            FROM Ticker t2
            WHERE t2.marketId IN :marketIds
            GROUP BY t2.marketId
        )
        """)
    List<Ticker> findLatestByMarketIds(@Param("marketIds") List<Integer> marketIds);

    /**
     * 특정 마켓의 최신 Ticker 조회
     *
     * @param marketId 마켓ID
     * @return 최신 Ticker
     */
    @Query("""
        SELECT t FROM Ticker t
        WHERE t.marketId = :marketId
        ORDER BY t.sourceCreatedAt DESC
        LIMIT 1
        """)
    Optional<Ticker> findLatestByMarketId(@Param("marketId") Integer marketId);

    @Modifying
    @Query(value = """
        INSERT INTO md_ticker (
            market_id, source_created_at, code,
            trade_price, signed_change_rate, signed_change_price,
            acc_trade_price, acc_trade_price_24h
        ) VALUES (
            :marketId, :sourceCreatedAt, :code,
            :tradePrice, :signedChangeRate, :signedChangePrice,
            :accTradePrice, :accTradePrice24h
        )
        ON DUPLICATE KEY UPDATE
            code = VALUES(code),
            trade_price = VALUES(trade_price),
            signed_change_rate = VALUES(signed_change_rate),
            signed_change_price = VALUES(signed_change_price),
            acc_trade_price = VALUES(acc_trade_price),
            acc_trade_price_24h = VALUES(acc_trade_price_24h)
        """, nativeQuery = true)
    void upsert(
        @Param("marketId") Integer marketId,
        @Param("sourceCreatedAt") LocalDateTime sourceCreatedAt,
        @Param("code") String code,
        @Param("tradePrice") BigDecimal tradePrice,
        @Param("signedChangeRate") BigDecimal signedChangeRate,
        @Param("signedChangePrice") BigDecimal signedChangePrice,
        @Param("accTradePrice") BigDecimal accTradePrice,
        @Param("accTradePrice24h") BigDecimal accTradePrice24h
    );
}
