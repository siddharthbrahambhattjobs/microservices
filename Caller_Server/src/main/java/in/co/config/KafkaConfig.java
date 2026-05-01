package in.co.config;

import java.util.Map;

import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import in.co.dto.StudentEvent;

@Configuration
public class KafkaConfig {

	@Bean
	ProducerFactory<String, StudentEvent> producerFactory(KafkaProperties kafkaProperties) {
		Map<String, Object> props = kafkaProperties.buildProducerProperties();
		// You can also manually add properties here if needed
		return new DefaultKafkaProducerFactory<>(props);
	}

	@Bean
	KafkaTemplate<String, StudentEvent> studentEventKafkaTemplate(
			ProducerFactory<String, StudentEvent> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}
}