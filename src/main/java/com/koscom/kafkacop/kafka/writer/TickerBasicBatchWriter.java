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
import java.util.concurrent.atomic.AtomicInteger;
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

		// 0. null 필터링 및 유효성 검증 (스킵 원인 추적)
		AtomicInteger nullMessages = new AtomicInteger(0);
		AtomicInteger invalidMktCode = new AtomicInteger(0);
		AtomicInteger invalidTimestamp = new AtomicInteger(0);
		AtomicInteger index = new AtomicInteger(0);

		List<TickerBasicMessage> validBatch = batch.stream()
			.filter(msg -> {
				int idx = index.getAndIncrement();
				if (msg == null) {
					nullMessages.incrementAndGet();
					log.error("[TickerBasic] NULL message detected at batch index {}! batchSize={}, batchHashCode={}",
						idx, batch.size(), System.identityHashCode(batch));
					return false;
				}
				return true;
			})
			.filter(msg -> {
				if (msg.mktCode() == null || msg.mktCode().isEmpty()) {
					invalidMktCode.incrementAndGet();
					log.warn("[TickerBasic] Invalid mktCode: msg={}", msg);
					return false;
				}
				return true;
			})
			.filter(msg -> {
				if (msg.timestamp() <= 0) {
					invalidTimestamp.incrementAndGet();
					log.warn("[TickerBasic] Invalid timestamp: timestamp={}, msg={}", msg.timestamp(), msg);
					return false;
				}
				return true;
			})
			.toList();

		if (validBatch.isEmpty()) {
			log.warn("[TickerBasic] All messages in batch were invalid (nullMsg={}, invalidMktCode={}, invalidTimestamp={})",
				nullMessages.get(), invalidMktCode.get(), invalidTimestamp.get());
			return;
		}

		// 스킵된 메시지 로깅
		if (nullMessages.get() > 0 || invalidMktCode.get() > 0 || invalidTimestamp.get() > 0) {
			log.warn("[TickerBasic] Validation failed: nullMsg={}, invalidMktCode={}, invalidTimestamp={}",
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
		List<TickerBasicMessage> processableBatch = validBatch.stream()
			.filter(msg -> {
				String marketCode = String.join("/", msg.mktCode());
				boolean exists = marketMap.containsKey(marketCode);
				if (!exists) {
					marketNotFound.incrementAndGet();
					log.warn("[TickerBasic] Market not found in DB: marketCode={}, exchange={}, msg={}",
						marketCode, exchangeCode, msg);
				}
				return exists;
			})
			.toList();

		if (processableBatch.isEmpty()) {
			log.warn("[TickerBasic] No valid markets found for batch (marketNotFound={}), skipping flush",
				marketNotFound.get());
			return;
		}

		// Market 미존재 메시지 로깅
		if (marketNotFound.get() > 0) {
			log.warn("[TickerBasic] Market not found: count={}", marketNotFound.get());
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

		int totalSkipped = batch.size() - processableBatch.size();
		if (totalSkipped > 0) {
			log.warn("[TickerBasic] SUMMARY - Skipped {}/{} messages: nullMsg={}, invalidMktCode={}, invalidTimestamp={}, marketNotFound={}",
				totalSkipped, batch.size(),
				nullMessages.get(), invalidMktCode.get(), invalidTimestamp.get(), marketNotFound.get());
		}
		log.debug("[TickerBasic] Flushed {} ticker-basic messages", processableBatch.size());
	}
}
