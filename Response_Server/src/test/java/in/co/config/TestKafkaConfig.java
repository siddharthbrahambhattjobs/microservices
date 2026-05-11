package in.co.config;

import static org.mockito.Mockito.mock;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import in.co.dto.StudentEvent;

/**
 * Test configuration to provide mock KafkaTemplate beans for @RetryableTopic support
 * in integration tests. This prevents context initialization errors when Kafka
 * is not available during testing.
 */
@TestConfiguration
public class TestKafkaConfig {

    @Bean
    public KafkaTemplate<String, StudentEvent> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }

    @Bean(name = "defaultRetryTopicKafkaTemplate")
    public KafkaTemplate<String, String> defaultKafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
}

