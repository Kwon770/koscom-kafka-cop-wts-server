package com.koscom.kafkacop.kafka.batch;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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

	// === 구성 요소 ===
	private final BlockingQueue<T> queue;
	private final List<T> buffer;
	private final BatchWriter<T> writer;                // DB upsert 수행
	private final String sourceTopic;                   // 원본 토픽명 (DLT 전송용)
	private final KafkaTemplate<String, Object> kafkaTemplate;  // DLT 전송용

	private ExecutorService coordinatorExecutor;        // 배치 수집 스레드
	private ExecutorService flushExecutorPool;          // 배치 flush 스레드 풀
	private volatile boolean running = true;

	public BatchAccumulator(BatchWriter<T> writer, int batchSize, Duration maxLatency, int queueCapacity,
	                        String sourceTopic, KafkaTemplate<String, Object> kafkaTemplate) {
		this(writer, batchSize, maxLatency, queueCapacity, 3, sourceTopic, kafkaTemplate);  // 기본 3개 워커
	}

	public BatchAccumulator(BatchWriter<T> writer, int batchSize, Duration maxLatency, int queueCapacity,
	                        int workerThreadCount, String sourceTopic, KafkaTemplate<String, Object> kafkaTemplate) {
		this.writer = writer;
		this.batchSize = batchSize;
		this.maxLatency = maxLatency;
		this.queueCapacity = queueCapacity;
		this.workerThreadCount = workerThreadCount;
		this.sourceTopic = sourceTopic;
		this.kafkaTemplate = kafkaTemplate;
		this.queue = new ArrayBlockingQueue<>(queueCapacity);
		this.buffer = new ArrayList<>(batchSize);
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
		if (!ok) {
			// 정책 선택: (A) 드롭/로깅 (B) 차단시간 늘리기 (C) DLT/알람
			log.warn("Accumulator queue full; dropped or route to fallback. item={}", item);
		}
	}

	@PostConstruct
	public void start() {
		// 1. 배치 수집 전담 스레드 (단일)
		coordinatorExecutor = Executors.newSingleThreadExecutor(r -> {
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

		coordinatorExecutor.submit(this::runLoop);
		log.info("BatchAccumulator started: batchSize={}, maxLatency={}ms, queueCapacity={}, workerThreads={}",
			batchSize, maxLatency.toMillis(), queueCapacity, workerThreadCount);
	}

	@PreDestroy
	public void stop() {
		running = false;
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
					// 2) 큐에 남은 것 한 번에 더 가져오기 (drainTo로 I/O 호출 수 최소화)
					queue.drainTo(buffer, batchSize - buffer.size());
				}

				boolean sizeReached = buffer.size() >= batchSize;
				boolean timeReached = !buffer.isEmpty() && (now - lastFlushNanos) >= maxWaitNanos;

				if (sizeReached || timeReached) {
					List<T> toFlush = new ArrayList<>(buffer);
					buffer.clear();
					lastFlushNanos = now;

					// 워커 풀에 비동기로 flush 작업 제출 (병렬 처리)
					flushExecutorPool.submit(() -> {
						try {
							writer.flush(toFlush); // DB 배치 UPSERT (트랜잭션 내부)
							log.debug("Batch flushed successfully: size={}", toFlush.size());
						} catch (Exception e) {
							// 실패 정책: 재시도/분할/로그/알람 (필요 시 DLT로 라우팅)
							log.error("Batch flush failed; size={}", toFlush.size(), e);
							// 간단 재시도 예시
							retryFlush(toFlush, 3);
						}
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
	 * 최종 실패한 배치를 DLT(Dead Letter Topic)로 전송
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
				kafkaTemplate.send(dltTopic, message);
				successCount++;
			} catch (Exception e) {
				log.error("Failed to send message to DLT topic={}", dltTopic, e);
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
