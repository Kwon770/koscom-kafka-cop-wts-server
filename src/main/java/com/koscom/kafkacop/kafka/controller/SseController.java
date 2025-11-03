package com.koscom.kafkacop.kafka.controller;

import com.koscom.kafkacop.kafka.sse.SseBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * SSE 실시간 스트리밍 API
 * - 카프카로부터 수신된 메시지를 실시간으로 클라이언트에게 전송
 */
@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

	private final SseBroadcaster sseBroadcaster;

	private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30분

	/**
	 * 특정 토픽 구독
	 * GET /api/sse/subscribe/{topic}
	 *
	 * 사용 가능한 토픽:
	 * - ticker-basic
	 * - ticker-extended
	 * - candel-1s
	 * - orderbook-5
	 */
	@GetMapping(value = "/subscribe/{topic}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(@PathVariable String topic) {
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

		// 구독 등록
		sseBroadcaster.subscribe(topic, emitter);

		// 연결 성공 메시지
		try {
			emitter.send(SseEmitter.event()
				.name("connected")
				.data("Successfully connected to topic: " + topic));
			log.info("SSE client connected to topic: {}", topic);
		} catch (IOException e) {
			log.error("Failed to send connection message", e);
			emitter.completeWithError(e);
		}

		return emitter;
	}

	/**
	 * 모든 토픽 구독
	 * GET /api/sse/subscribe/all
	 */
	@GetMapping(value = "/subscribe/all", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribeAll() {
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

		// 모든 토픽에 구독
		sseBroadcaster.subscribe("ticker-basic", emitter);
		sseBroadcaster.subscribe("ticker-extended", emitter);
		sseBroadcaster.subscribe("candel-1s", emitter);
		sseBroadcaster.subscribe("orderbook-5", emitter);

		// 연결 성공 메시지
		try {
			emitter.send(SseEmitter.event()
				.name("connected")
				.data("Successfully connected to all topics"));
			log.info("SSE client connected to all topics");
		} catch (IOException e) {
			log.error("Failed to send connection message", e);
			emitter.completeWithError(e);
		}

		return emitter;
	}

	/**
	 * SSE 구독자 통계 조회
	 * GET /api/sse/stats
	 */
	@GetMapping("/stats")
	public SseStats getStats() {
		return new SseStats(
			sseBroadcaster.getTotalSubscribers(),
			sseBroadcaster.getTopicSubscribers("ticker-basic"),
			sseBroadcaster.getTopicSubscribers("ticker-extended"),
			sseBroadcaster.getTopicSubscribers("candel-1s"),
			sseBroadcaster.getTopicSubscribers("orderbook-5")
		);
	}

	public record SseStats(
		int totalSubscribers,
		int tickerBasicSubscribers,
		int tickerExtendedSubscribers,
		int candleSecondSubscribers,
		int orderbook5Subscribers
	) {}
}