package com.koscom.kafkacop.kafka.batch.writer;

import com.koscom.kafkacop.chart.repository.CandleRepository;
import com.koscom.kafkacop.kafka.batch.BatchAccumulator;
import com.koscom.kafkacop.kafka.dto.CandleSecondMessage;
import com.koscom.kafkacop.market.domain.Market;
import com.koscom.kafkacop.market.repository.MarketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandleSecondBatchWriter implements BatchAccumulator.BatchWriter<CandleSecondMessage> {

	private final MarketRepository marketRepository;
	private final JdbcTemplate jdbcTemplate;

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	@Override
	@Transactional
	public void flush(List<CandleSecondMessage> batch) {
		if (batch == null || batch.isEmpty()) {
			return;
		}

		// 0. null 필터링 및 유효성 검증
		List<CandleSecondMessage> validBatch = batch.stream()
			.filter(msg -> msg != null)
			.filter(msg -> msg.mktCode() != null && !msg.mktCode().isEmpty())
			.filter(msg -> msg.candleDateTimeKst() != null)
			.toList();

		if (validBatch.isEmpty()) {
			log.warn("All messages in batch were null or invalid, skipping flush");
			return;
		}

		// 1. 거래소명과 마켓코드 추출
		String exchangeCode = validBatch.get(0).exchange();
		List<String> marketCodes = validBatch.stream()
			.map(msg -> String.join("/", msg.mktCode()))
			.distinct()
			.toList();

		// 2. Market 조회 (배치)
		Map<String, Market> marketMap = marketRepository
			.findAllByExchange_ExchangeCodeAndMarketCodeIn(exchangeCode, marketCodes)
			.stream()
			.collect(Collectors.toMap(Market::getMarketCode, m -> m));

		// 3. Market이 존재하는 메시지만 필터링
		List<CandleSecondMessage> processableBatch = validBatch.stream()
			.filter(msg -> {
				String marketCode = String.join("/", msg.mktCode());
				return marketMap.containsKey(marketCode);
			})
			.toList();

		if (processableBatch.isEmpty()) {
			log.warn("No valid markets found for batch, skipping flush");
			return;
		}

		// 4. JDBC Batch UPSERT
		String sql = """
			INSERT INTO md_candle (
				market_id, candle_date_time, code, type,
				opening_price, high_price, low_price, trade_price,
				candle_acc_trade_volume, candle_acc_trade_price
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			ON DUPLICATE KEY UPDATE
				code = VALUES(code),
				type = VALUES(type),
				opening_price = VALUES(opening_price),
				high_price = VALUES(high_price),
				low_price = VALUES(low_price),
				trade_price = VALUES(trade_price),
				candle_acc_trade_volume = VALUES(candle_acc_trade_volume),
				candle_acc_trade_price = VALUES(candle_acc_trade_price)
			""";

		jdbcTemplate.batchUpdate(sql, processableBatch, processableBatch.size(), (ps, msg) -> {
			String marketCode = String.join("/", msg.mktCode());
			Market market = marketMap.get(marketCode);
			LocalDateTime candleKst = LocalDateTime.parse(msg.candleDateTimeKst(), FORMATTER);

			int idx = 1;
			ps.setInt(idx++, market.getMarketId());
			ps.setObject(idx++, candleKst);
			ps.setString(idx++, marketCode);
			ps.setString(idx++, "candle.1s");
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.openingPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.highPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.lowPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.tradePrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.candleAccTradeVolume()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.candleAccTradePrice()));
		});

		int skippedCount = batch.size() - processableBatch.size();
		if (skippedCount > 0) {
			log.warn("Skipped {} invalid candle-1s messages", skippedCount);
		}
		log.info("Flushed {} candle-1s messages", processableBatch.size());
	}
}
