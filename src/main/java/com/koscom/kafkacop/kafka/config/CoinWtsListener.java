package com.koscom.kafkacop.kafka.config;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.koscom.kafkacop.kafka.dto.CandleSecondMessage;
import com.koscom.kafkacop.kafka.dto.Orderbook5Message;
import com.koscom.kafkacop.kafka.dto.TickerBasicMessage;
import com.koscom.kafkacop.kafka.dto.TickerExtendMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CoinWtsListener {

	@KafkaListener(topics = "ticker-basic", containerFactory = "tickerBasicListenerContainerFactory")
	public void onTickerBasicBatch(List<ConsumerRecord<String, TickerBasicMessage>> records, Acknowledgment ack) {
		try {
			for (ConsumerRecord<String, TickerBasicMessage> record : records) {
				TickerBasicMessage message = record.value();
				log.info("Received ticker-basic message: {}", message);
			}
			ack.acknowledge();
		} catch (Exception e) {
			throw e;
		}
	}

	@KafkaListener(topics = "candel-1s", containerFactory = "candleSecondListenerContainerFactory")
	public void onCandleSecondBatch(List<ConsumerRecord<String, CandleSecondMessage>> records, Acknowledgment ack) {
		try {
			for (ConsumerRecord<String, CandleSecondMessage> record : records) {
				CandleSecondMessage message = record.value();
				log.info("Received candel-1s message: {}", message);
			}
			ack.acknowledge();
		} catch (Exception e) {
			throw e;
		}
	}

	@KafkaListener(topics = "orderbook-5", containerFactory = "orderbook5ListenerContainerFactory")
	public void onOrderbook5Batch(List<ConsumerRecord<String, Orderbook5Message>> records, Acknowledgment ack) {
		try {
			for (ConsumerRecord<String, Orderbook5Message> record : records) {
				Orderbook5Message message = record.value();
				log.info("Received orderbook-5 message: {}", message);
			}
			ack.acknowledge();
		} catch (Exception e) {
			throw e;
		}
	}

	@KafkaListener(topics = "ticker-extended", containerFactory = "tickerExtendedListenerContainerFactory")
	public void onTickerExtendedBatch(List<ConsumerRecord<String, TickerExtendMessage>> records, Acknowledgment ack) {
		try {
			for (ConsumerRecord<String, TickerExtendMessage> record : records) {
				TickerExtendMessage message = record.value();
				log.info("Received ticker-extended message: {}", message);
			}
			ack.acknowledge();
		} catch (Exception e) {
			throw e;
		}
	}
}
