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

		// 0. null 필터링 및 유효성 검증
		List<Orderbook5Message> validBatch = batch.stream()
			.filter(msg -> msg != null)
			.filter(msg -> msg.mktCode() != null && !msg.mktCode().isEmpty())
			.filter(msg -> msg.orderbookUnits() != null && msg.orderbookUnits().size() >= 5)
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
		List<Orderbook5Message> processableBatch = validBatch.stream()
			.filter(msg -> {
				String marketCode = String.join("/", msg.mktCode());
				return marketMap.containsKey(marketCode);
			})
			.toList();

		if (processableBatch.isEmpty()) {
			log.warn("No valid markets found for batch, skipping flush");
			return;
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

		int skippedCount = batch.size() - processableBatch.size();
		if (skippedCount > 0) {
			log.warn("Skipped {} invalid orderbook-5 messages", skippedCount);
		}
		log.debug("Flushed {} orderbook-5 messages", processableBatch.size());
	}
}
