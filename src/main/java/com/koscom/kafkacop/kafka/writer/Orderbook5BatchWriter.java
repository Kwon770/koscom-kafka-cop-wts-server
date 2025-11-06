package com.koscom.kafkacop.kafka.writer;

import com.koscom.kafkacop.kafka.dto.Orderbook5Message;
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
public class Orderbook5BatchWriter implements BatchAccumulator.BatchWriter<Orderbook5Message> {

	private final MarketRepository marketRepository;
	private final JdbcTemplate jdbcTemplate;

	@Override
	@Transactional
	public void flush(List<Orderbook5Message> batch) {
		if (batch == null || batch.isEmpty()) {
			return;
		}

		// 0. null 필터링 및 유효성 검증 (스킵 원인 추적)
		AtomicInteger nullMessages = new AtomicInteger(0);
		AtomicInteger invalidMktCode = new AtomicInteger(0);
		AtomicInteger invalidOrderbookUnits = new AtomicInteger(0);
		AtomicInteger invalidTimestamp = new AtomicInteger(0);
		AtomicInteger index = new AtomicInteger(0);

		List<Orderbook5Message> validBatch = batch.stream()
			.filter(msg -> {
				int idx = index.getAndIncrement();
				if (msg == null) {
					nullMessages.incrementAndGet();
					log.error("[Orderbook5] NULL message detected at batch index {}! batchSize={}, batchHashCode={}",
						idx, batch.size(), System.identityHashCode(batch));
					return false;
				}
				return true;
			})
			.filter(msg -> {
				if (msg.mktCode() == null || msg.mktCode().isEmpty()) {
					invalidMktCode.incrementAndGet();
					log.warn("[Orderbook5] Invalid mktCode: msg={}", msg);
					return false;
				}
				return true;
			})
			.filter(msg -> {
				if (msg.orderbookUnits() == null || msg.orderbookUnits().size() < 5) {
					invalidOrderbookUnits.incrementAndGet();
					log.warn("[Orderbook5] Invalid orderbookUnits (need 5): size={}, msg={}",
						msg.orderbookUnits() != null ? msg.orderbookUnits().size() : 0, msg);
					return false;
				}
				return true;
			})
			.filter(msg -> {
				if (msg.timestamp() <= 0) {
					invalidTimestamp.incrementAndGet();
					log.warn("[Orderbook5] Invalid timestamp: timestamp={}, msg={}", msg.timestamp(), msg);
					return false;
				}
				return true;
			})
			.toList();

		if (validBatch.isEmpty()) {
			log.warn("[Orderbook5] All messages in batch were invalid (nullMsg={}, invalidMktCode={}, invalidUnits={}, invalidTimestamp={})",
				nullMessages.get(), invalidMktCode.get(), invalidOrderbookUnits.get(), invalidTimestamp.get());
			return;
		}

		// 스킵된 메시지 로깅
		if (nullMessages.get() > 0 || invalidMktCode.get() > 0 || invalidOrderbookUnits.get() > 0 || invalidTimestamp.get() > 0) {
			log.warn("[Orderbook5] Validation failed: nullMsg={}, invalidMktCode={}, invalidUnits={}, invalidTimestamp={}",
				nullMessages.get(), invalidMktCode.get(), invalidOrderbookUnits.get(), invalidTimestamp.get());
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
		List<Orderbook5Message> processableBatch = validBatch.stream()
			.filter(msg -> {
				String marketCode = String.join("/", msg.mktCode());
				boolean exists = marketMap.containsKey(marketCode);
				if (!exists) {
					marketNotFound.incrementAndGet();
					log.warn("[Orderbook5] Market not found in DB: marketCode={}, exchange={}, msg={}",
						marketCode, exchangeCode, msg);
				}
				return exists;
			})
			.toList();

		if (processableBatch.isEmpty()) {
			log.warn("[Orderbook5] No valid markets found for batch (marketNotFound={}), skipping flush",
				marketNotFound.get());
			return;
		}

		// Market 미존재 메시지 로깅
		if (marketNotFound.get() > 0) {
			log.warn("[Orderbook5] Market not found: count={}", marketNotFound.get());
		}

		// 4. JDBC Batch UPSERT (진짜 배치 처리)
		String sql = """
			INSERT INTO md_ob_top5 (
				market_id, orderbook_date_time, code,
				total_ask_size, total_bid_size,
				ask_p1, ask_q1, bid_p1, bid_q1,
				ask_p2, ask_q2, bid_p2, bid_q2,
				ask_p3, ask_q3, bid_p3, bid_q3,
				ask_p4, ask_q4, bid_p4, bid_q4,
				ask_p5, ask_q5, bid_p5, bid_q5
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
			""";

		jdbcTemplate.batchUpdate(sql, processableBatch, processableBatch.size(), (ps, msg) -> {
			String marketCode = String.join("/", msg.mktCode());
			Market market = marketMap.get(marketCode);
			List<Orderbook5Message.OrderbookUnit> units = msg.orderbookUnits();

			// timestamp를 자동으로 마이크로초/밀리초 구분하여 KST LocalDateTime으로 변환
			LocalDateTime orderbookDateTime = TimestampConverter.toLocalDateTimeKst(msg.timestamp());

			int idx = 1;
			ps.setInt(idx++, market.getMarketId());
			ps.setObject(idx++, orderbookDateTime);
			ps.setString(idx++, marketCode);
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.totalAskSize()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(msg.totalBidSize()));
			// 1호가
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(0).askPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(0).askSize()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(0).bidPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(0).bidSize()));
			// 2호가
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(1).askPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(1).askSize()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(1).bidPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(1).bidSize()));
			// 3호가
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(2).askPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(2).askSize()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(2).bidPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(2).bidSize()));
			// 4호가
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(3).askPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(3).askSize()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(3).bidPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(3).bidSize()));
			// 5호가
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(4).askPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(4).askSize()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(4).bidPrice()));
			ps.setBigDecimal(idx++, BigDecimal.valueOf(units.get(4).bidSize()));
		});

		int totalSkipped = batch.size() - processableBatch.size();
		if (totalSkipped > 0) {
			log.warn("[Orderbook5] SUMMARY - Skipped {}/{} messages: nullMsg={}, invalidMktCode={}, invalidUnits={}, invalidTimestamp={}, marketNotFound={}",
				totalSkipped, batch.size(),
				nullMessages.get(), invalidMktCode.get(), invalidOrderbookUnits.get(), invalidTimestamp.get(), marketNotFound.get());
		}
		log.debug("[Orderbook5] Flushed {} orderbook-5 messages", processableBatch.size());
	}
}
