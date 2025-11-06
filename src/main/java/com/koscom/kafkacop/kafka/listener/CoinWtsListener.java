package com.koscom.kafkacop.kafka.listener;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.koscom.kafkacop.kafka.writer.BatchAccumulator;
import com.koscom.kafkacop.kafka.dto.CandleSecondMessage;
import com.koscom.kafkacop.kafka.dto.Orderbook5Message;
import com.koscom.kafkacop.kafka.dto.TickerBasicMessage;
import com.koscom.kafkacop.kafka.sse.SseBroadcaster;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoinWtsListener {

	private final SseBroadcaster sseBroadcaster;
	private final BatchAccumulator<TickerBasicMessage> tickerBasicAccumulator;
	private final BatchAccumulator<CandleSecondMessage> candleSecondAccumulator;
	private final BatchAccumulator<Orderbook5Message> orderbook5Accumulator;
	private final MeterRegistry meterRegistry;

	// 메트릭
	private Counter tickerBasicConsumedCounter;
	private Counter candleSecondConsumedCounter;
	private Counter orderbook5ConsumedCounter;

	@PostConstruct
	public void initializeMetrics() {
		tickerBasicConsumedCounter = Counter.builder("kafka.consumer.messages.consumed")
			.tag("topic", "ticker-basic")
			.description("Number of messages consumed from ticker-basic topic")
			.register(meterRegistry);

		candleSecondConsumedCounter = Counter.builder("kafka.consumer.messages.consumed")
			.tag("topic", "candel-1s")
			.description("Number of messages consumed from candel-1s topic")
			.register(meterRegistry);

		orderbook5ConsumedCounter = Counter.builder("kafka.consumer.messages.consumed")
			.tag("topic", "orderbook-5")
			.description("Number of messages consumed from orderbook-5 topic")
			.register(meterRegistry);
	}

	/**
	 * 배치 소비 → 즉시 SSE 전송 + 배치 DB 저장 패턴
	 * - 리스너에서는 SSE 브로드캐스트와 배치 누적만 수행 (지연 최소화)
	 * - DB 저장은 별도 워커 스레드가 N/T 조건으로 배치 처리
	 */
	@KafkaListener(topics = "ticker-basic", containerFactory = "tickerBasicListenerContainerFactory")
	public void onTickerBasic(List<ConsumerRecord<String, TickerBasicMessage>> records, Acknowledgment ack) {
		try {
			int processedCount = 0;
			int nullCount = 0;

			for (ConsumerRecord<String, TickerBasicMessage> record : records) {
				TickerBasicMessage message = record.value();

				// 역직렬화 실패 감지 (null 체크)
				if (message == null) {
					nullCount++;
					log.error("[ticker-basic] Deserialization failed - NULL message received! " +
						"topic={}, partition={}, offset={}, key={}, timestamp={}, headers={}",
						record.topic(), record.partition(), record.offset(), record.key(),
						record.timestamp(), record.headers());
					// null 메시지는 스킵하고 계속 진행
					continue;
				}

				// 1) 즉시 SSE 브로드캐스트 (저지연)
				sseBroadcaster.broadcast("ticker-basic", message);

				// 2) 배치 저장용 큐에 적재 (비차단)
				tickerBasicAccumulator.add(message);

				processedCount++;
			}

			// 3) 배치 전체 처리 완료 후 오프셋 커밋
			ack.acknowledge();

			// 4) 메트릭 기록 (배치 단위)
			tickerBasicConsumedCounter.increment(processedCount);

			log.debug("Processed ticker-basic batch: total={}, processed={}, null={}",
				records.size(), processedCount, nullCount);
		} catch (Exception e) {
			log.error("Failed to process ticker-basic batch", e);
			throw e;
		}
	}

	@KafkaListener(topics = "candel-1s", containerFactory = "candleSecondListenerContainerFactory")
	public void onCandleSecond(List<ConsumerRecord<String, CandleSecondMessage>> records, Acknowledgment ack) {
		try {
			int processedCount = 0;
			int nullCount = 0;

			for (ConsumerRecord<String, CandleSecondMessage> record : records) {
				CandleSecondMessage message = record.value();

				// 역직렬화 실패 감지 (null 체크)
				if (message == null) {
					nullCount++;
					log.error("[candel-1s] Deserialization failed - NULL message received! " +
						"topic={}, partition={}, offset={}, key={}, timestamp={}, headers={}",
						record.topic(), record.partition(), record.offset(), record.key(),
						record.timestamp(), record.headers());
					// null 메시지는 스킵하고 계속 진행
					continue;
				}

				// 1) 즉시 SSE 브로드캐스트
				sseBroadcaster.broadcast("candel-1s", message);

				// 2) 배치 저장용 큐에 적재
				candleSecondAccumulator.add(message);

				processedCount++;
			}

			// 3) 배치 전체 처리 완료 후 오프셋 커밋
			ack.acknowledge();

			// 4) 메트릭 기록 (배치 단위)
			candleSecondConsumedCounter.increment(processedCount);

			log.debug("Processed candel-1s batch: total={}, processed={}, null={}",
				records.size(), processedCount, nullCount);
		} catch (Exception e) {
			log.error("Failed to process candel-1s batch", e);
			throw e;
		}
	}

	@KafkaListener(topics = "orderbook-5", containerFactory = "orderbook5ListenerContainerFactory")
	public void onOrderbook5(List<ConsumerRecord<String, Orderbook5Message>> records, Acknowledgment ack) {
		try {
			int processedCount = 0;
			int nullCount = 0;

			for (ConsumerRecord<String, Orderbook5Message> record : records) {
				Orderbook5Message message = record.value();

				// 역직렬화 실패 감지 (null 체크)
				if (message == null) {
					nullCount++;
					log.error("[orderbook-5] Deserialization failed - NULL message received! " +
						"topic={}, partition={}, offset={}, key={}, timestamp={}, headers={}",
						record.topic(), record.partition(), record.offset(), record.key(),
						record.timestamp(), record.headers());
					// null 메시지는 스킵하고 계속 진행
					continue;
				}

				// 1) 즉시 SSE 브로드캐스트
				sseBroadcaster.broadcast("orderbook-5", message);

				// 2) 배치 저장용 큐에 적재
				orderbook5Accumulator.add(message);

				processedCount++;
			}

			// 3) 배치 전체 처리 완료 후 오프셋 커밋
			ack.acknowledge();

			// 4) 메트릭 기록 (배치 단위)
			orderbook5ConsumedCounter.increment(processedCount);

			log.debug("Processed orderbook-5 batch: total={}, processed={}, null={}",
				records.size(), processedCount, nullCount);
		} catch (Exception e) {
			log.error("Failed to process orderbook-5 batch", e);
			throw e;
		}
	}
}
