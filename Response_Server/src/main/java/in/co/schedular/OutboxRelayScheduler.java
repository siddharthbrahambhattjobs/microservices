package in.co.schedular;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import in.co.dto.OutboxEvent;
import in.co.repo.OutboxEventRepository;

@Component
public class OutboxRelayScheduler {

	private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

	private static final int BATCH_SIZE = 50;
	private static final int MAX_RETRY_COUNT = 5;

	private final OutboxEventRepository outboxRepository;
	// In OutboxRelayScheduler — inject by name
	@Autowired
	@Qualifier("outboxKafkaTemplate")
	private KafkaTemplate<String, String> kafkaTemplate;

	public OutboxRelayScheduler(OutboxEventRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
		this.outboxRepository = outboxRepository;
		this.kafkaTemplate = kafkaTemplate;
	}

	@Scheduled(fixedDelay = 1000)
	public void processOutboxEvents() {
		List<OutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING",
				PageRequest.of(0, BATCH_SIZE));

		for (OutboxEvent event : pendingEvents) {
			try {
				kafkaTemplate.send("student-events", event.getAggregateId(), event.getPayload()).get();

				event.setStatus("PUBLISHED");
				event.setPublishedAt(LocalDateTime.now());
				event.setLastError(null);

				outboxRepository.save(event);

				log.info("Outbox event published successfully. eventId={}, aggregateId={}, retryCount={}",
						event.getId(), event.getAggregateId(), event.getRetryCount());

			} catch (Exception e) {
				int updatedRetryCount = event.getRetryCount() + 1;
				event.setRetryCount(updatedRetryCount);
				event.setLastError(truncateError(e));

				if (updatedRetryCount >= MAX_RETRY_COUNT) {
					event.setStatus("FAILED");
				} else {
					event.setStatus("PENDING");
				}

				outboxRepository.save(event);

				log.error("Outbox publish failed. eventId={}, aggregateId={}, retryCount={}, status={}", event.getId(),
						event.getAggregateId(), event.getRetryCount(), event.getStatus(), e);

				if ("PENDING".equals(event.getStatus())) {
					break;
				}
			}
		}
	}

	private String truncateError(Exception e) {
		String message = e.getMessage();
		if (message == null || message.isBlank()) {
			message = e.getClass().getName();
		}
		return message.length() > 2000 ? message.substring(0, 2000) : message;
	}
}