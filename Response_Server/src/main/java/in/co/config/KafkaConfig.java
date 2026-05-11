package in.co.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import in.co.dto.StudentEvent;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ✅ Named exactly 'defaultRetryTopicKafkaTemplate'
    // Spring Kafka's @RetryableTopic looks for this specific bean name
    @Bean(name = "defaultRetryTopicKafkaTemplate")
    @Primary  // ✅ Also marks as primary — resolves ambiguity on @Autowired
    public KafkaTemplate<String, String> defaultRetryTopicKafkaTemplate() {
        return new KafkaTemplate<>(stringProducerFactory());
    }

    // ✅ For OutboxRelayScheduler — raw String JSON payloads
    // Same underlying factory — no duplicate connection overhead
    @Bean(name = "outboxKafkaTemplate")
    public KafkaTemplate<String, String> outboxKafkaTemplate() {
        return new KafkaTemplate<>(stringProducerFactory());
    }

    // ✅ For ResponseController — typed StudentEvent payloads
    @Bean(name = "studentEventKafkaTemplate")
    public KafkaTemplate<String, StudentEvent> studentEventKafkaTemplate() {
        return new KafkaTemplate<>(studentEventProducerFactory());
    }

    // ── Producer Factories ────────────────────────────────────────────────

    private ProducerFactory<String, String> stringProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // ✅ Idempotent producer — prevents duplicate messages on retry
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(props);
    }

    private ProducerFactory<String, StudentEvent> studentEventProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // ✅ Don't include type header — avoids deserialization issues cross-service
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(props);
    }
}