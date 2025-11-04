package com.koscom.kafkacop.chart.repository;

import com.koscom.kafkacop.chart.domain.Candle;
import com.koscom.kafkacop.chart.domain.CandleId;
import com.koscom.kafkacop.chart.domain.CandleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CandleRepository extends JpaRepository<Candle, CandleId> {

    @Modifying
    @Query(value = """
        INSERT INTO md_candle (
            market_id, candle_date_time, code, type,
            opening_price, high_price, low_price, trade_price,
            candle_acc_trade_volume, candle_acc_trade_price
        ) VALUES (
            :marketId, :candleDateTime, :code, :type,
            :openingPrice, :highPrice, :lowPrice, :tradePrice,
            :candleAccTradeVolume, :candleAccTradePrice
        )
        ON DUPLICATE KEY UPDATE
            code = VALUES(code),
            type = VALUES(type),
            opening_price = VALUES(opening_price),
            high_price = VALUES(high_price),
            low_price = VALUES(low_price),
            trade_price = VALUES(trade_price),
            candle_acc_trade_volume = VALUES(candle_acc_trade_volume),
            candle_acc_trade_price = VALUES(candle_acc_trade_price)
        """, nativeQuery = true)
    void upsert(
        @Param("marketId") Integer marketId,
        @Param("candleDateTime") LocalDateTime candleDateTime,
        @Param("code") String code,
        @Param("type") String type,
        @Param("openingPrice") BigDecimal openingPrice,
        @Param("highPrice") BigDecimal highPrice,
        @Param("lowPrice") BigDecimal lowPrice,
        @Param("tradePrice") BigDecimal tradePrice,
        @Param("candleAccTradeVolume") BigDecimal candleAccTradeVolume,
        @Param("candleAccTradePrice") BigDecimal candleAccTradePrice
    );

    /**
     * 거래소 코드, 마켓 코드, 타입으로 시간 범위 내의 캔들 조회 (오름차순)
     * N+1 방지를 위해 FETCH JOIN 사용
     */
    @Query("SELECT c FROM Candle c " +
           "JOIN FETCH c.market m " +
           "JOIN FETCH m.exchange e " +
           "WHERE e.exchangeCode = :exchangeCode AND c.code = :code AND c.type = :type " +
           "AND c.id.candleDateTime >= :from AND c.id.candleDateTime < :to " +
           "ORDER BY c.id.candleDateTime ASC")
    List<Candle> findByExchangeCodeAndCodeAndTypeAndDateTimeBetween(
        @Param("exchangeCode") String exchangeCode,
        @Param("code") String code,
        @Param("type") CandleType type,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    /**
     * 특정 마켓 코드와 타입으로 시간 범위 내의 캔들 조회 (오름차순)
     * @deprecated exchange 파라미터를 함께 사용해야 함
     */
    @Deprecated
    @Query("SELECT c FROM Candle c WHERE c.code = :code AND c.type = :type " +
           "AND c.id.candleDateTime >= :from AND c.id.candleDateTime < :to " +
           "ORDER BY c.id.candleDateTime ASC")
    List<Candle> findByCodeAndTypeAndDateTimeBetween(
        @Param("code") String code,
        @Param("type") CandleType type,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}
