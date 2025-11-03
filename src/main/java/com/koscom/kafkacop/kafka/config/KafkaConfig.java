package com.koscom.kafkacop.kafka.config;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.koscom.kafkacop.kafka.dto.CandleSecondMessage;
import com.koscom.kafkacop.kafka.dto.Orderbook5Message;
import com.koscom.kafkacop.kafka.dto.TickerBasicMessage;

@EnableKafka
@Configuration
public class KafkaConfig {

	private Map<String, Object> getBaseConsumerProps(KafkaProperties kafkaProperties, SslBundles sslBundles) {
		Map<String, Object> props = kafkaProperties.buildConsumerProperties(sslBundles);
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
		props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
		return props;
	}

	@Bean
	public ConsumerFactory<String, TickerBasicMessage> tickerBasicConsumerFactory(KafkaProperties kafkaProperties, SslBundles sslBundles) {
		Map<String, Object> props = getBaseConsumerProps(kafkaProperties, sslBundles);
		props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TickerBasicMessage.class.getName());
		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean
	public ConsumerFactory<String, CandleSecondMessage> candleSecondConsumerFactory(KafkaProperties kafkaProperties, SslBundles sslBundles) {
		Map<String, Object> props = getBaseConsumerProps(kafkaProperties, sslBundles);
		props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, CandleSecondMessage.class.getName());
		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean
	public ConsumerFactory<String, Orderbook5Message> orderbook5ConsumerFactory(KafkaProperties kafkaProperties, SslBundles sslBundles) {
		Map<String, Object> props = getBaseConsumerProps(kafkaProperties, sslBundles);
		props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Orderbook5Message.class.getName());
		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean
	public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties, SslBundles sslBundles) {
		Map<String, Object> props = kafkaProperties.buildProducerProperties(sslBundles);
		return new DefaultKafkaProducerFactory<>(props);
	}

	@Bean
	public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}

	private <T> ConcurrentKafkaListenerContainerFactory<String, T> createContainerFactory(
		ConsumerFactory<String, T> consumerFactory, KafkaTemplate<String, Object> kafkaTemplate
	) {
		ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		factory.setBatchListener(false); // 단건 소비로 변경 (즉시 SSE 전송 + 배치 DB 저장)

		// Acknowledgment 파라미터 사용을 위해 MANUAL 모드 설정
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

		// 재시도/백오프 설정
		ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3); // 최대 3회 재시도
		backOff.setInitialInterval(100L); // 초기 대기 시간 100ms
		backOff.setMultiplier(2.0); // 대기 시간 배수
		backOff.setMaxInterval(1000L); // 최대 대기 시간 1초

		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
			kafkaTemplate,
			(cr, ex) -> new TopicPartition(cr.topic() + ".DLT", cr.partition())
		);

		DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
		errorHandler.setAckAfterHandle(false);
		factory.setCommonErrorHandler(errorHandler);

		return factory;
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, TickerBasicMessage> tickerBasicListenerContainerFactory(
		ConsumerFactory<String, TickerBasicMessage> tickerBasicConsumerFactory, KafkaTemplate<String, Object> kafkaTemplate
	) {
		return createContainerFactory(tickerBasicConsumerFactory, kafkaTemplate);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, CandleSecondMessage> candleSecondListenerContainerFactory(
		ConsumerFactory<String, CandleSecondMessage> candleSecondConsumerFactory, KafkaTemplate<String, Object> kafkaTemplate
	) {
		return createContainerFactory(candleSecondConsumerFactory, kafkaTemplate);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, Orderbook5Message> orderbook5ListenerContainerFactory(
		ConsumerFactory<String, Orderbook5Message> orderbook5ConsumerFactory, KafkaTemplate<String, Object> kafkaTemplate
	) {
		return createContainerFactory(orderbook5ConsumerFactory, kafkaTemplate);
	}
}
