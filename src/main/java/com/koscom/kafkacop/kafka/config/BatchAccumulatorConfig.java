package com.koscom.kafkacop.kafka.config;

import com.koscom.kafkacop.kafka.batch.BatchAccumulator;
import com.koscom.kafkacop.kafka.batch.writer.CandleSecondBatchWriter;
import com.koscom.kafkacop.kafka.batch.writer.Orderbook5BatchWriter;
import com.koscom.kafkacop.kafka.batch.writer.TickerBasicBatchWriter;
import com.koscom.kafkacop.kafka.dto.CandleSecondMessage;
import com.koscom.kafkacop.kafka.dto.Orderbook5Message;
import com.koscom.kafkacop.kafka.dto.TickerBasicMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

/**
 * BatchAccumulator 설정
 * - 각 메시지 타입별로 별도의 accumulator 생성
 * - 튜닝 파라미터: batchSize, maxLatency, queueCapacity
 * - DLT: 최종 실패 시 {topic}-dlt로 전송
 */
@Configuration
public class BatchAccumulatorConfig {

	// === 튜닝 파라미터 ===
	private static final int BATCH_SIZE = 3000;              // 배치 최대 개수 (1000 → 3000으로 증가)
	private static final Duration MAX_LATENCY = Duration.ofMillis(100);
	private static final int QUEUE_CAPACITY = 200_000;        // 내부 큐 용량 (100,000 → 200,000으로 증가)

	@Bean
	public BatchAccumulator<TickerBasicMessage> tickerBasicAccumulator(
		TickerBasicBatchWriter writer,
		KafkaTemplate<String, Object> kafkaTemplate
	) {
		BatchAccumulator<TickerBasicMessage> accumulator = new BatchAccumulator<>(
			writer,
			BATCH_SIZE,
			MAX_LATENCY,
			QUEUE_CAPACITY,
			"ticker-basic",
			kafkaTemplate
		);
		accumulator.start();
		return accumulator;
	}

	@Bean
	public BatchAccumulator<CandleSecondMessage> candleSecondAccumulator(
		CandleSecondBatchWriter writer,
		KafkaTemplate<String, Object> kafkaTemplate
	) {
		BatchAccumulator<CandleSecondMessage> accumulator = new BatchAccumulator<>(
			writer,
			BATCH_SIZE,
			MAX_LATENCY,
			QUEUE_CAPACITY,
			"candel-1s",
			kafkaTemplate
		);
		accumulator.start();
		return accumulator;
	}

	@Bean
	public BatchAccumulator<Orderbook5Message> orderbook5Accumulator(
		Orderbook5BatchWriter writer,
		KafkaTemplate<String, Object> kafkaTemplate
	) {
		BatchAccumulator<Orderbook5Message> accumulator = new BatchAccumulator<>(
			writer,
			BATCH_SIZE,
			MAX_LATENCY,
			QUEUE_CAPACITY,
			"orderbook-5",
			kafkaTemplate
		);
		accumulator.start();
		return accumulator;
	}
}
