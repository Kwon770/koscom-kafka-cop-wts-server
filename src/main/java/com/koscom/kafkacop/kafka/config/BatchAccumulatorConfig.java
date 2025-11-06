package com.koscom.kafkacop.kafka.config;

import com.koscom.kafkacop.kafka.writer.BatchAccumulator;
import com.koscom.kafkacop.kafka.writer.CandleSecondBatchWriter;
import com.koscom.kafkacop.kafka.writer.Orderbook5BatchWriter;
import com.koscom.kafkacop.kafka.writer.TickerBasicBatchWriter;
import com.koscom.kafkacop.kafka.dto.CandleSecondMessage;
import com.koscom.kafkacop.kafka.dto.Orderbook5Message;
import com.koscom.kafkacop.kafka.dto.TickerBasicMessage;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

/**
 * BatchAccumulator 설정
 * - 각 메시지 타입별로 별도의 accumulator 생성
 * - 튜닝 파라미터: application.yml의 app.batch-accumulator에서 설정
 * - DLT: 최종 실패 시 {topic}.DLT로 전송
 */
@Configuration
public class BatchAccumulatorConfig {

	@Value("${app.batch-accumulator.batch-size}")
	private int batchSize;

	@Value("${app.batch-accumulator.max-latency-ms}")
	private long maxLatencyMs;

	@Value("${app.batch-accumulator.queue-capacity}")
	private int queueCapacity;

	@Value("${app.batch-accumulator.worker-thread-count}")
	private int workerThreadCount;

	@Value("${app.batch-accumulator.coordinator-thread-count}")
	private int coordinatorThreadCount;

	@Bean
	public BatchAccumulator<TickerBasicMessage> tickerBasicAccumulator(
		TickerBasicBatchWriter writer,
		KafkaTemplate<String, Object> kafkaTemplate,
		MeterRegistry meterRegistry
	) {
		BatchAccumulator<TickerBasicMessage> accumulator = new BatchAccumulator<>(
			writer,
			batchSize,
			Duration.ofMillis(maxLatencyMs),
			queueCapacity,
			workerThreadCount,
			coordinatorThreadCount,
			"ticker-basic",
			kafkaTemplate,
			meterRegistry
		);
		accumulator.start();
		return accumulator;
	}

	@Bean
	public BatchAccumulator<CandleSecondMessage> candleSecondAccumulator(
		CandleSecondBatchWriter writer,
		KafkaTemplate<String, Object> kafkaTemplate,
		MeterRegistry meterRegistry
	) {
		BatchAccumulator<CandleSecondMessage> accumulator = new BatchAccumulator<>(
			writer,
			batchSize,
			Duration.ofMillis(maxLatencyMs),
			queueCapacity,
			workerThreadCount,
			coordinatorThreadCount,
			"candel-1s",
			kafkaTemplate,
			meterRegistry
		);
		accumulator.start();
		return accumulator;
	}

	@Bean
	public BatchAccumulator<Orderbook5Message> orderbook5Accumulator(
		Orderbook5BatchWriter writer,
		KafkaTemplate<String, Object> kafkaTemplate,
		MeterRegistry meterRegistry
	) {
		BatchAccumulator<Orderbook5Message> accumulator = new BatchAccumulator<>(
			writer,
			batchSize,
			Duration.ofMillis(maxLatencyMs),
			queueCapacity,
			workerThreadCount,
			coordinatorThreadCount,
			"orderbook-5",
			kafkaTemplate,
			meterRegistry
		);
		accumulator.start();
		return accumulator;
	}
}
