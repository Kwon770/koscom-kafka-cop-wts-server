package com.koscom.kafkacop.chart.repository;

import com.koscom.kafkacop.chart.domain.Candle;
import com.koscom.kafkacop.chart.domain.CandleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}
