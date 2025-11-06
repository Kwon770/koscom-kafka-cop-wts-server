package com.koscom.kafkacop.kafka.writer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 단건 수신 → 큐 적재 → 워커가 N/T 조건으로 배치 flush.
 * - 리스너(컨슈머) 스레드: add() 만 수행 (지연 최소화)
 * - 워커 스레드: 큐 poll/drain → 배치 구성 → DB writer.flush()
 */
@Slf4j
public class BatchAccumulator<T> {

	// === 튜닝 파라미터 ===
	private final int batchSize;                        // N: 배치 최대 개수
	private final Duration maxLatency;                  // T: 배치 최대 대기시간
	private final int queueCapacity;                    // 내부 큐 용량(백프레셔 임계)
	private final int workerThreadCount;                // 워커 스레드 수 (병렬 처리)
	private final int coordinatorThreadCount;           // 코디네이터 스레드 수 (큐 소비)

	// === 구성 요소 ===
	private final BlockingQueue<T> queue;
	private final List<T> buffer;
	private final BatchWriter<T> writer;                // DB upsert 수행
	private final String sourceTopic;                   // 원본 토픽명 (DLT 전송용)
	private final KafkaTemplate<String, Object> kafkaTemplate;  // DLT 전송용
	private final MeterRegistry meterRegistry;          // 메트릭 레지스트리

	private ExecutorService coordinatorExecutor;        // 배치 수집 스레드
	private ExecutorService flushExecutorPool;          // 배치 flush 스레드 풀
	private ScheduledExecutorService metricsScheduler;  // 메트릭 업데이트 스케줄러
	private volatile boolean running = true;

	// === 메트릭 ===
	private Counter messagesQueuedCounter;              // 큐에 추가된 메시지 수
	private Counter messagesDroppedCounter;             // 큐 가득 차서 드롭된 메시지 수
	private Counter batchesProcessedCounter;            // 처리된 배치 수
	private Counter messagesProcessedCounter;           // 처리된 메시지 수
	private Counter coordinatorProcessedCounter;        // Coordinator가 처리한 메시지 수
	private Timer flushTimer;                           // flush 소요 시간

	// === 분당 처리량 추적 (per-minute throughput) ===
	private final AtomicLong messagesQueuedLastMinute = new AtomicLong(0);
	private final AtomicLong coordinatorProcessedLastMinute = new AtomicLong(0);
	private final AtomicLong workerProcessedLastMinute = new AtomicLong(0);
	private final AtomicLong messagesQueuedPerMinute = new AtomicLong(0);
	private final AtomicLong coordinatorThroughputPerMinute = new AtomicLong(0);
	private final AtomicLong workerThroughputPerMinute = new AtomicLong(0);
	private volatile long lastResetTime = System.currentTimeMillis();

	public BatchAccumulator(BatchWriter<T> writer, int batchSize, Duration maxLatency, int queueCapacity,
	                        String sourceTopic, KafkaTemplate<String, Object> kafkaTemplate,
	                        MeterRegistry meterRegistry) {
		this(writer, batchSize, maxLatency, queueCapacity, 3, 1, sourceTopic, kafkaTemplate, meterRegistry);
	}

	public BatchAccumulator(BatchWriter<T> writer, int batchSize, Duration maxLatency, int queueCapacity,
	                        int workerThreadCount, String sourceTopic, KafkaTemplate<String, Object> kafkaTemplate,
	                        MeterRegistry meterRegistry) {
		this(writer, batchSize, maxLatency, queueCapacity, workerThreadCount, 1, sourceTopic, kafkaTemplate, meterRegistry);
	}

	public BatchAccumulator(BatchWriter<T> writer, int batchSize, Duration maxLatency, int queueCapacity,
	                        int workerThreadCount, int coordinatorThreadCount,
	                        String sourceTopic, KafkaTemplate<String, Object> kafkaTemplate,
	                        MeterRegistry meterRegistry) {
		this.writer = writer;
		this.batchSize = batchSize;
		this.maxLatency = maxLatency;
		this.queueCapacity = queueCapacity;
		this.workerThreadCount = workerThreadCount;
		this.coordinatorThreadCount = coordinatorThreadCount;
		this.sourceTopic = sourceTopic;
		this.kafkaTemplate = kafkaTemplate;
		this.meterRegistry = meterRegistry;
		this.queue = new ArrayBlockingQueue<>(queueCapacity);
		this.buffer = new ArrayList<>(batchSize);

		// 메트릭 초기화
		initializeMetrics();
	}

	private void initializeMetrics() {
		String topicTag = sourceTopic != null ? sourceTopic : "unknown";

		// 큐 크기 게이지 (현재 큐에 대기 중인 메시지 수)
		meterRegistry.gaugeCollectionSize(
			"batch.accumulator.queue.size",
			io.micrometer.core.instrument.Tags.of("topic", topicTag),
			queue
		);

		// 큐 용량 게이지 (최대 큐 크기)
		Gauge.builder("batch.accumulator.queue.capacity", () -> (double) queueCapacity)
			.tag("topic", topicTag)
			.description("Maximum capacity of the message queue")
			.register(meterRegistry);

		// 큐 사용률 게이지 (0.0 ~ 1.0, 즉 0% ~ 100%)
		Gauge.builder("batch.accumulator.queue.usage_ratio", () -> (double) queue.size() / queueCapacity)
			.tag("topic", topicTag)
			.description("Queue usage ratio (0.0 to 1.0)")
			.register(meterRegistry);

		// 큐 여유 공간 게이지
		Gauge.builder("batch.accumulator.queue.remaining", () -> (double) (queueCapacity - queue.size()))
			.tag("topic", topicTag)
			.description("Remaining space in the queue")
			.register(meterRegistry);

		// 카운터 초기화
		messagesQueuedCounter = Counter.builder("batch.accumulator.messages.queued")
			.tag("topic", topicTag)
			.description("Number of messages added to the queue")
			.register(meterRegistry);

		messagesDroppedCounter = Counter.builder("batch.accumulator.messages.dropped")
			.tag("topic", topicTag)
			.description("Number of messages dropped due to queue full")
			.register(meterRegistry);

		batchesProcessedCounter = Counter.builder("batch.accumulator.batches.processed")
			.tag("topic", topicTag)
			.description("Number of batches flushed to database")
			.register(meterRegistry);

		messagesProcessedCounter = Counter.builder("batch.accumulator.messages.processed")
			.tag("topic", topicTag)
			.description("Number of messages flushed to database")
			.register(meterRegistry);

		coordinatorProcessedCounter = Counter.builder("batch.accumulator.coordinator.processed")
			.tag("topic", topicTag)
			.description("Number of messages processed by coordinator (polled from queue)")
			.register(meterRegistry);

		flushTimer = Timer.builder("batch.accumulator.flush.duration")
			.tag("topic", topicTag)
			.description("Time taken to flush a batch to database")
			.register(meterRegistry);

		// 분당 처리량 게이지 (Kafka Consumer Thread)
		Gauge.builder("batch.accumulator.consumer.throughput.per_minute", messagesQueuedPerMinute, AtomicLong::get)
			.tag("topic", topicTag)
			.description("Messages received from Kafka per minute (consumer thread throughput)")
			.register(meterRegistry);

		// 분당 처리량 게이지 (Coordinator Thread)
		Gauge.builder("batch.accumulator.coordinator.throughput.per_minute", coordinatorThroughputPerMinute, AtomicLong::get)
			.tag("topic", topicTag)
			.description("Messages coordinated per minute (coordinator thread throughput)")
			.register(meterRegistry);

		// 분당 처리량 게이지 (Worker Thread)
		Gauge.builder("batch.accumulator.worker.throughput.per_minute", workerThroughputPerMinute, AtomicLong::get)
			.tag("topic", topicTag)
			.description("Messages flushed to DB per minute (worker thread throughput)")
			.register(meterRegistry);
	}

	/** 리스너(단건 소비)에서 호출: 매우 빠르게 끝나야 함 */
	public void add(T item) {
		// 정책 1) 초고신뢰: 대기해서라도 반드시 적재 (offer with timeout)
		boolean ok;
		try {
			ok = queue.offer(item, 5, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			ok = false;
		}
		if (ok) {
			messagesQueuedCounter.increment();
			messagesQueuedLastMinute.incrementAndGet();  // 분당 처리량 추적
		} else {
			// 큐 가득참: DLT로 전송
			messagesDroppedCounter.increment();
			log.warn("[{}] Accumulator queue full (capacity={}); sending message to DLT. item={}",
				sourceTopic, queueCapacity, item);

			// DLT로 단건 전송 (비동기)
			sendSingleMessageToDlt(item);
		}
	}

	@PostConstruct
	public void start() {
		// 경고: coordinator가 여러 개일 경우 buffer thread-safety 문제 발생 가능
		if (coordinatorThreadCount > 1) {
			log.warn("coordinatorThreadCount > 1 is experimental and may cause thread-safety issues with buffer. " +
				"Recommended value is 1. Current value: {}", coordinatorThreadCount);
		}

		// 1. 배치 수집 코디네이터 스레드 풀
		coordinatorExecutor = Executors.newFixedThreadPool(coordinatorThreadCount, r -> {
			Thread t = new Thread(r, "batch-coordinator-" + System.identityHashCode(this));
			t.setDaemon(true);
			return t;
		});

		// 2. 배치 flush 워커 풀 (병렬 처리)
		flushExecutorPool = Executors.newFixedThreadPool(workerThreadCount, r -> {
			Thread t = new Thread(r, "batch-flush-worker-" + System.identityHashCode(this));
			t.setDaemon(true);
			return t;
		});

		// 3. 메트릭 업데이트 스케줄러 (5초마다 업데이트, 1분마다 리셋)
		metricsScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "batch-metrics-updater-" + System.identityHashCode(this));
			t.setDaemon(true);
			return t;
		});

		// 5초마다 메트릭 업데이트 (현재 누적값 반영)
		// 1분마다 자동으로 리셋 (내부 로직에서 처리)
		metricsScheduler.scheduleAtFixedRate(this::updateThroughputMetrics, 5, 5, TimeUnit.SECONDS);

		// 코디네이터 수만큼 runLoop 실행
		for (int i = 0; i < coordinatorThreadCount; i++) {
			coordinatorExecutor.submit(this::runLoop);
		}

		log.debug("BatchAccumulator started: batchSize={}, maxLatency={}ms, queueCapacity={}, " +
			"coordinatorThreads={}, workerThreads={}",
			batchSize, maxLatency.toMillis(), queueCapacity, coordinatorThreadCount, workerThreadCount);
	}

	/**
	 * 5초마다 실행: 현재 누적된 처리량을 게이지에 업데이트
	 * 1분마다: 누적 카운터를 0으로 리셋하여 새로운 1분 측정 시작
	 */
	private void updateThroughputMetrics() {
		try {
			long currentTime = System.currentTimeMillis();
			long elapsedSeconds = (currentTime - lastResetTime) / 1000;

			// 현재 누적값을 게이지에 설정 (리셋하지 않음)
			long consumerThroughput = messagesQueuedLastMinute.get();
			long coordinatorThroughput = coordinatorProcessedLastMinute.get();
			long workerThroughput = workerProcessedLastMinute.get();

			messagesQueuedPerMinute.set(consumerThroughput);
			coordinatorThroughputPerMinute.set(coordinatorThroughput);
			workerThroughputPerMinute.set(workerThroughput);

			// 1분(60초) 경과 시 리셋
			if (elapsedSeconds >= 60) {
				messagesQueuedLastMinute.set(0);
				coordinatorProcessedLastMinute.set(0);
				workerProcessedLastMinute.set(0);
				lastResetTime = currentTime;

				log.debug("[{}] Throughput metrics RESET after 1 minute: consumer={}/min, coordinator={}/min, worker={}/min, queue={}",
					sourceTopic, consumerThroughput, coordinatorThroughput, workerThroughput, queue.size());
			} else {
				log.debug("[{}] Throughput metrics updated ({}s): consumer={}, coordinator={}, worker={}, queue={}",
					sourceTopic, elapsedSeconds, consumerThroughput, coordinatorThroughput, workerThroughput, queue.size());
			}
		} catch (Exception e) {
			log.error("Failed to update throughput metrics", e);
		}
	}

	@PreDestroy
	public void stop() {
		running = false;

		// 메트릭 스케줄러 종료
		if (metricsScheduler != null) {
			metricsScheduler.shutdownNow();
		}

		coordinatorExecutor.shutdownNow();
		try {
			coordinatorExecutor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}

		// 남은 것 최종 flush
		flushRemainder();

		// flush 워커 풀 종료
		flushExecutorPool.shutdown();
		try {
			flushExecutorPool.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}

		log.info("BatchAccumulator stopped");
	}

	/** 워커 스레드 메인 루프: N 또는 T 조건으로 flush */
	private void runLoop() {
		long lastFlushNanos = System.nanoTime();
		final long maxWaitNanos = maxLatency.toNanos();

		try {
			while (running) {
				// 1) 첫 건을 poll (최대 maxLatency 대기) → 없으면 타임아웃으로 주기적 flush 기회
				T first = queue.poll(maxLatency.toMillis(), TimeUnit.MILLISECONDS);
				long now = System.nanoTime();

				if (first != null) {
					buffer.add(first);
					int coordinatorProcessedCount = 1;  // poll로 1개 가져옴

					// 2) 큐에 남은 것 한 번에 더 가져오기 (drainTo로 I/O 호출 수 최소화)
					int drained = queue.drainTo(buffer, batchSize - buffer.size());
					coordinatorProcessedCount += drained;

					// Coordinator 처리량 메트릭 기록
					coordinatorProcessedCounter.increment(coordinatorProcessedCount);
					coordinatorProcessedLastMinute.addAndGet(coordinatorProcessedCount);
				}

				boolean sizeReached = buffer.size() >= batchSize;
				boolean timeReached = !buffer.isEmpty() && (now - lastFlushNanos) >= maxWaitNanos;

				if (sizeReached || timeReached) {
					List<T> toFlush = new ArrayList<>(buffer);
					int currentBatchSize = toFlush.size();
					buffer.clear();
					lastFlushNanos = now;

					// 워커 풀에 비동기로 flush 작업 제출 (병렬 처리)
					flushExecutorPool.submit(() -> {
						flushTimer.record(() -> {
							try {
								writer.flush(toFlush); // DB 배치 UPSERT (트랜잭션 내부)

								// 메트릭 기록
								batchesProcessedCounter.increment();
								messagesProcessedCounter.increment(currentBatchSize);
								workerProcessedLastMinute.addAndGet(currentBatchSize);

								log.debug("Batch flushed successfully: size={}", currentBatchSize);
							} catch (Exception e) {
								// 실패 정책: 재시도/분할/로그/알람 (필요 시 DLT로 라우팅)
								log.error("Batch flush failed; size={}", currentBatchSize, e);
								// 간단 재시도 예시
								retryFlush(toFlush, 3);
							}
						});
					});
				}
			}
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		} finally {
			flushRemainder();
		}
	}

	private void retryFlush(List<T> batch, int maxRetry) {
		int attempt = 0;
		while (attempt++ < maxRetry) {
			try {
				writer.flush(batch);
				log.info("Retry {}/{} succeeded", attempt, maxRetry);
				return;
			} catch (Exception e) {
				log.warn("Retry {}/{} failed", attempt, maxRetry, e);
				sleepQuiet(50L * attempt);
			}
		}
		// 최종 실패 시 DLT로 전송
		log.error("Permanent failure after {} attempts; sending {} messages to DLT", maxRetry, batch.size());
		sendToDlt(batch);
	}

	/**
	 * 큐 오버플로우 시 단건 메시지를 DLT로 즉시 전송 (비동기, 비차단)
	 * - 리스너 스레드를 차단하지 않도록 fire-and-forget 방식
	 */
	private void sendSingleMessageToDlt(T message) {
		if (kafkaTemplate == null || sourceTopic == null) {
			log.warn("KafkaTemplate or sourceTopic not configured; cannot send to DLT");
			return;
		}

		String dltTopic = sourceTopic + ".DLT";

		// 비동기 전송 (리스너 스레드를 차단하지 않음)
		kafkaTemplate.send(dltTopic, message)
			.whenComplete((result, ex) -> {
				if (ex != null) {
					log.error("[{}] Failed to send dropped message to DLT topic={}, message={}",
						sourceTopic, dltTopic, message, ex);
				} else {
					log.info("[{}] Successfully sent dropped message to DLT topic={}, offset={}",
						sourceTopic, dltTopic, result.getRecordMetadata().offset());
				}
			});
	}

	/**
	 * 최종 실패한 배치를 DLT(Dead Letter Topic)로 전송
	 * - 각 메시지의 전체 value 데이터를 JSON으로 직렬화하여 전송
	 * - 동기적으로 전송 결과를 확인하여 성공/실패 여부 판단
	 */
	private void sendToDlt(List<T> batch) {
		if (kafkaTemplate == null || sourceTopic == null) {
			log.warn("KafkaTemplate or sourceTopic not configured; cannot send to DLT");
			return;
		}

		String dltTopic = sourceTopic + ".DLT";
		int successCount = 0;
		int failureCount = 0;

		for (T message : batch) {
			try {
				// 동기적으로 전송하고 결과 확인 (블로킹)
				// message 객체 전체가 JSON으로 직렬화되어 value로 전송됨
				kafkaTemplate.send(dltTopic, message)
					.whenComplete((result, ex) -> {
						if (ex != null) {
							log.error("Failed to send message to DLT topic={}, message={}", dltTopic, message, ex);
						} else {
							log.debug("Successfully sent message to DLT topic={}, offset={}",
								dltTopic, result.getRecordMetadata().offset());
						}
					})
					.get(5, TimeUnit.SECONDS);  // 최대 5초 대기
				successCount++;
			} catch (Exception e) {
				log.error("Failed to send message to DLT topic={}, message={}", dltTopic, message, e);
				failureCount++;
			}
		}

		log.info("Sent {} messages to DLT topic={} (success={}, failure={})",
			batch.size(), dltTopic, successCount, failureCount);
	}

	private void sleepQuiet(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}
	}

	private void flushRemainder() {
		if (!buffer.isEmpty()) {
			try {
				writer.flush(new ArrayList<>(buffer));
				log.info("Final buffer flush: size={}", buffer.size());
			} catch (Exception e) {
				log.error("Final flush failed", e);
			}
			buffer.clear();
		}
		List<T> tail = new ArrayList<>(queue.size());
		queue.drainTo(tail);
		if (!tail.isEmpty()) {
			try {
				writer.flush(tail);
				log.info("Final drain flush: size={}", tail.size());
			} catch (Exception e) {
				log.error("Final drain flush failed", e);
			}
		}
	}

	/** DB 벌크 저장기: JPA 네이티브 UPSERT 등을 수행 */
	public interface BatchWriter<T> {
		void flush(List<T> batch);
	}
}
