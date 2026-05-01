package in.co.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	// Topic for initiating the Saga
	@Bean
	NewTopic studentCreateCommandsTopic() {
		return TopicBuilder.name("student-create-commands")
				// 3 partitions allow up to 3 instances of the Response Service
				// to consume commands concurrently in the same consumer group.
				.partitions(3)
				// 1 replica is sufficient for your local single-broker setup.
				// Increase this to 3 for production environments.
				.replicas(1).build();
	}

	// Topic for broadcasting the Saga result (Success or Failure)
	@Bean
	NewTopic studentEventsTopic() {
		return TopicBuilder.name("student-events")
				// 3 partitions allow up to 3 instances of the Caller Service
				// to process the asynchronous results concurrently.
				.partitions(3).replicas(1).build();
	}
}