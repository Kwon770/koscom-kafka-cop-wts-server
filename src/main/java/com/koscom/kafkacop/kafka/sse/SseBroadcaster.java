package com.koscom.kafkacop.kafka.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * SSE 브로드캐스터: 카프카 메시지를 연결된 모든 클라이언트에게 실시간 전송
 */
@Slf4j
@Component
public class SseBroadcaster {

	// 토픽별로 구독자 관리
	private final Map<String, CopyOnWriteArraySet<SseEmitter>> topicEmitters = new ConcurrentHashMap<>();

	/**
	 * 새로운 SSE 연결 등록
	 * @param topic 구독할 토픽
	 * @param emitter SSE emitter
	 */
	public void subscribe(String topic, SseEmitter emitter) {
		topicEmitters.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>()).add(emitter);

		emitter.onCompletion(() -> unsubscribe(topic, emitter));
		emitter.onTimeout(() -> unsubscribe(topic, emitter));
		emitter.onError(e -> unsubscribe(topic, emitter));

		log.debug("SSE client subscribed to topic: {}, total subscribers: {}", topic, topicEmitters.get(topic).size());
	}

	/**
	 * SSE 연결 해제
	 */
	private void unsubscribe(String topic, SseEmitter emitter) {
		CopyOnWriteArraySet<SseEmitter> emitters = topicEmitters.get(topic);
		if (emitters != null) {
			emitters.remove(emitter);
			log.debug("SSE client unsubscribed from topic: {}, remaining subscribers: {}", topic, emitters.size());
		}
	}

	/**
	 * 특정 토픽의 모든 구독자에게 메시지 브로드캐스트
	 * @param topic 토픽명
	 * @param message 전송할 메시지
	 */
	public void broadcast(String topic, Object message) {
		CopyOnWriteArraySet<SseEmitter> emitters = topicEmitters.get(topic);
		if (emitters == null || emitters.isEmpty()) {
			return;
		}

		// 실패한 emitter 목록
		CopyOnWriteArraySet<SseEmitter> deadEmitters = new CopyOnWriteArraySet<>();

		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event()
					.name(topic)
					.data(message));
			} catch (IOException e) {
				log.warn("Failed to send SSE message to client, removing emitter: {}", e.getMessage());
				deadEmitters.add(emitter);
			} catch (Exception e) {
				log.error("Unexpected error while sending SSE message", e);
				deadEmitters.add(emitter);
			}
		}

		// 실패한 emitter 제거
		deadEmitters.forEach(emitter -> unsubscribe(topic, emitter));
	}

	/**
	 * 연결된 총 구독자 수 반환
	 */
	public int getTotalSubscribers() {
		return topicEmitters.values().stream()
			.mapToInt(CopyOnWriteArraySet::size)
			.sum();
	}

	/**
	 * 특정 토픽의 구독자 수 반환
	 */
	public int getTopicSubscribers(String topic) {
		CopyOnWriteArraySet<SseEmitter> emitters = topicEmitters.get(topic);
		return emitters != null ? emitters.size() : 0;
	}
}