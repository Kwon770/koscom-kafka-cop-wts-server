package com.koscom.kafkacop.kafka.writer;

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
import java.util.concurrent.atomic.AtomicInteger;
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

		// 0. null 필터링 및 유효성 검증 (스킵 원인 추적)
		AtomicInteger nullMessages = new AtomicInteger(0);
		AtomicInteger invalidMktCode = new AtomicInteger(0);
		AtomicInteger invalidTimestamp = new AtomicInteger(0);
		AtomicInteger index = new AtomicInteger(0);

		List<CandleSecondMessage> validBatch = batch.stream()
			.filter(msg -> {
				int idx = index.getAndIncrement();
				if (msg == null) {
					nullMessages.incrementAndGet();
					log.error("[CandleSecond] NULL message detected at batch index {}! batchSize={}, batchHashCode={}",
						idx, batch.size(), System.identityHashCode(batch));
					return false;
				}
				return true;
			})
			.filter(msg -> {
				if (msg.mktCode() == null || msg.mktCode().isEmpty()) {
					invalidMktCode.incrementAndGet();
					log.warn("[CandleSecond] Invalid mktCode: msg={}", msg);
					return false;
				}
				return true;
			})
			.filter(msg -> {
				if (msg.candleDateTimeKst() == null || msg.candleDateTimeKst().isEmpty()) {
					invalidTimestamp.incrementAndGet();
					log.warn("[CandleSecond] Invalid candleDateTimeKst: msg={}", msg);
					return false;
				}
				return true;
			})
			.toList();

		if (validBatch.isEmpty()) {
			log.warn("[CandleSecond] All messages in batch were invalid (nullMsg={}, invalidMktCode={}, invalidTimestamp={})",
				nullMessages.get(), invalidMktCode.get(), invalidTimestamp.get());
			return;
		}

		// 스킵된 메시지 로깅
		if (nullMessages.get() > 0 || invalidMktCode.get() > 0 || invalidTimestamp.get() > 0) {
			log.warn("[CandleSecond] Validation failed: nullMsg={}, invalidMktCode={}, invalidTimestamp={}",
				nullMessages.get(), invalidMktCode.get(), invalidTimestamp.get());
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

		// 3. Market이 존재하는 메시지만 필터링 (Market 미존재 추적)
		AtomicInteger marketNotFound = new AtomicInteger(0);
		List<CandleSecondMessage> processableBatch = validBatch.stream()
			.filter(msg -> {
				String marketCode = String.join("/", msg.mktCode());
				boolean exists = marketMap.containsKey(marketCode);
				if (!exists) {
					marketNotFound.incrementAndGet();
					log.warn("[CandleSecond] Market not found in DB: marketCode={}, exchange={}, msg={}",
						marketCode, exchangeCode, msg);
				}
				return exists;
			})
			.toList();

		if (processableBatch.isEmpty()) {
			log.warn("[CandleSecond] No valid markets found for batch (marketNotFound={}), skipping flush",
				marketNotFound.get());
			return;
		}

		// Market 미존재 메시지 로깅
		if (marketNotFound.get() > 0) {
			log.warn("[CandleSecond] Market not found: count={}", marketNotFound.get());
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
			ps.setString(idx++, "1s");  // CandleType.ONE_SECOND
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.openingPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.highPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.lowPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.tradePrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.candleAccTradeVolume()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.candleAccTradePrice()));
		});

		int totalSkipped = batch.size() - processableBatch.size();
		if (totalSkipped > 0) {
			log.warn("[CandleSecond] SUMMARY - Skipped {}/{} messages: nullMsg={}, invalidMktCode={}, invalidTimestamp={}, marketNotFound={}",
				totalSkipped, batch.size(),
				nullMessages.get(), invalidMktCode.get(), invalidTimestamp.get(), marketNotFound.get());
		}
		log.debug("[CandleSecond] Flushed {} candle-1s messages", processableBatch.size());
	}
}
