package com.koscom.kafkacop.kafka.writer;

import com.koscom.kafkacop.kafka.dto.TickerBasicMessage;
import com.koscom.kafkacop.kafka.util.TimestampConverter;
import com.koscom.kafkacop.market.domain.Market;
import com.koscom.kafkacop.market.repository.MarketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TickerBasicBatchWriter implements BatchAccumulator.BatchWriter<TickerBasicMessage> {

	private final MarketRepository marketRepository;
	private final JdbcTemplate jdbcTemplate;

	@Override
	@Transactional
	public void flush(List<TickerBasicMessage> batch) {
		if (batch == null || batch.isEmpty()) {
			return;
		}

		// 0. null 필터링 및 유효성 검증
		List<TickerBasicMessage> validBatch = batch.stream()
			.filter(msg -> msg != null)
			.filter(msg -> msg.mktCode() != null && !msg.mktCode().isEmpty())
			.filter(msg -> msg.timestamp() > 0)  // timestamp 검증 (PK의 일부)
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
		List<TickerBasicMessage> processableBatch = validBatch.stream()
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
			INSERT INTO md_ticker (
				market_id, source_created_at, code,
				trade_price, signed_change_rate, signed_change_price,
				acc_trade_price, acc_trade_price_24h
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
			ON DUPLICATE KEY UPDATE
				code = VALUES(code),
				trade_price = VALUES(trade_price),
				signed_change_rate = VALUES(signed_change_rate),
				signed_change_price = VALUES(signed_change_price),
				acc_trade_price = VALUES(acc_trade_price),
				acc_trade_price_24h = VALUES(acc_trade_price_24h)
			""";

		jdbcTemplate.batchUpdate(sql, processableBatch, processableBatch.size(), (ps, msg) -> {
			String marketCode = String.join("/", msg.mktCode());
			Market market = marketMap.get(marketCode);

			// timestamp를 자동으로 마이크로초/밀리초 구분하여 KST LocalDateTime으로 변환
			LocalDateTime sourceCreatedAt = TimestampConverter.toLocalDateTimeKst(msg.timestamp());

			int idx = 1;
			ps.setInt(idx++, market.getMarketId());
			ps.setObject(idx++, sourceCreatedAt);
			ps.setString(idx++, marketCode);
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.tradePrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.signedChangeRate()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.signedChangePrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.accTradePrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.accTradePrice24h()));
		});

		int skippedCount = batch.size() - processableBatch.size();
		if (skippedCount > 0) {
			log.warn("Skipped {} invalid ticker-basic messages", skippedCount);
		}
		log.info("Flushed {} ticker-basic messages", processableBatch.size());
	}
}
