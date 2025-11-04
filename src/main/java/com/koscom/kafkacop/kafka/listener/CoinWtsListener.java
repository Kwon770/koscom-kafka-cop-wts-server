package com.koscom.kafkacop.kafka.listener;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class CoinWtsListener {

	private final SseBroadcaster sseBroadcaster;
	private final BatchAccumulator<TickerBasicMessage> tickerBasicAccumulator;
	private final BatchAccumulator<CandleSecondMessage> candleSecondAccumulator;
	private final BatchAccumulator<Orderbook5Message> orderbook5Accumulator;

	/**
	 * 단건 소비 → 즉시 SSE 전송 + 배치 DB 저장 패턴
	 * - 리스너에서는 SSE 브로드캐스트와 배치 누적만 수행 (지연 최소화)
	 * - DB 저장은 별도 워커 스레드가 N/T 조건으로 배치 처리
	 */
	@KafkaListener(topics = "ticker-basic", containerFactory = "tickerBasicListenerContainerFactory")
	public void onTickerBasic(ConsumerRecord<String, TickerBasicMessage> record, Acknowledgment ack) {
		try {
			TickerBasicMessage message = record.value();

			// 1) 즉시 SSE 브로드캐스트 (저지연)
			sseBroadcaster.broadcast("ticker-basic", message);

			// 2) 배치 저장용 큐에 적재 (비차단)
			tickerBasicAccumulator.add(message);

			// 3) 오프셋 커밋: SSE 전송 + 큐 적재 성공 = 처리 성공
			ack.acknowledge();

			log.debug("Processed ticker-basic message: marketCode={}", String.join("/", message.mktCode()));
		} catch (Exception e) {
			log.error("Failed to process ticker-basic message", e);
			throw e;
		}
	}

	@KafkaListener(topics = "candel-1s", containerFactory = "candleSecondListenerContainerFactory")
	public void onCandleSecond(ConsumerRecord<String, CandleSecondMessage> record, Acknowledgment ack) {
		try {
			CandleSecondMessage message = record.value();

			// 1) 즉시 SSE 브로드캐스트
			sseBroadcaster.broadcast("candel-1s", message);

			// 2) 배치 저장용 큐에 적재
			candleSecondAccumulator.add(message);

			// 3) 오프셋 커밋
			ack.acknowledge();

			log.debug("Processed candel-1s message: marketCode={}", String.join("/", message.mktCode()));
		} catch (Exception e) {
			log.error("Failed to process candel-1s message", e);
			throw e;
		}
	}

	@KafkaListener(topics = "orderbook-5", containerFactory = "orderbook5ListenerContainerFactory")
	public void onOrderbook5(ConsumerRecord<String, Orderbook5Message> record, Acknowledgment ack) {
		try {
			Orderbook5Message message = record.value();

			// 1) 즉시 SSE 브로드캐스트
			sseBroadcaster.broadcast("orderbook-5", message);

			// 2) 배치 저장용 큐에 적재
			orderbook5Accumulator.add(message);

			// 3) 오프셋 커밋
			ack.acknowledge();

			log.debug("Processed orderbook-5 message: marketCode={}", String.join("/", message.mktCode()));
		} catch (Exception e) {
			log.error("Failed to process orderbook-5 message", e);
			throw e;
		}
	}
}
