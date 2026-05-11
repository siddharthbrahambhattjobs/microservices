package in.co.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.co.dto.Student;
import in.co.dto.StudentEvent;
import in.co.service.IdempotencyService;
import in.co.service.StudentService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/response")
public class ResponseController {

	private static final Logger log = LoggerFactory.getLogger(ResponseController.class);

	private final StudentService service;
	@Autowired
	@Qualifier("studentEventKafkaTemplate")
	private KafkaTemplate<String, StudentEvent> kafkaTemplate;
	private final StudentService studentService;
	private final IdempotencyService idempotencyService;

	public ResponseController(StudentService service, KafkaTemplate<String, StudentEvent> kafkaTemplate,
			IdempotencyService idempotencyService, StudentService studentService) {
		this.service = service;
		this.kafkaTemplate = kafkaTemplate;
		this.idempotencyService = idempotencyService;
		this.studentService = studentService;
	}

	@GetMapping("/getStudents/{id}")
	public ResponseEntity<Student> getStudent(@PathVariable int id) {
		return service.getStudent(id).map(student -> {
			log.info("Student from DB is {}", student);
			return ResponseEntity.ok(student);
		}).orElseGet(() -> {
			log.warn("Student with ID {} not found in DB", id);
			return ResponseEntity.notFound().build();
		});
	}

	@GetMapping("/getAllStudent")
	public ResponseEntity<List<Student>> getAllStudent() {
		var students = service.getAllStudent();
		return ResponseEntity.ok(students);
	}

	@RetryableTopic(attempts = "4", backOff = @BackOff(delay = 1000, multiplier = 1.5, maxDelay = 15000))
	@KafkaListener(topics = "student-create-commands", groupId = "response-service-group")
	public void processCreateStudentCommand(StudentEvent commandEvent) {
		String correlationId = commandEvent.correlationId();
		Student student = commandEvent.student();

		if (!idempotencyService.claim(correlationId)) {
			log.info("Duplicate/in-flight command ignored. correlationId={}", correlationId);
			return;
		}

		try {
			Student created = studentService.createStudentAndOutboxEvent(student, correlationId);
			idempotencyService.markCompleted(correlationId);
			log.info("Command processed successfully. studentId={}, correlationId={}", created.getId(), correlationId);

		} catch (DataIntegrityViolationException duplicate) {
			log.info("Duplicate command already persisted. correlationId={}", correlationId);
			idempotencyService.markCompleted(correlationId);

		} catch (Exception ex) {
			idempotencyService.release(correlationId);
			throw ex;
		}
	}

	/**
	 * L2 Standard DLT Handler: This acts as the final safety net after all 4
	 * retries are exhausted. We capture the headers to know exactly why and where
	 * it failed.
	 */
	@DltHandler
	public void handleDlt(StudentEvent commandEvent, @Header(KafkaHeaders.RECEIVED_TOPIC) String originalTopic,
			@Header(name = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {

		String correlationId = commandEvent.correlationId();
		var student = commandEvent.student();

		log.error("DLT Triggered: All retries exhausted for CorrelationId: {} from topic: {}. Error: {}", correlationId,
				originalTopic, errorMessage);

		try {
			// 1. Mark the student as FAILED for the downstream Saga Orchestrator
			student.setStatus("FAILED");
			String kafkaKey = (student.getId() != null) ? String.valueOf(student.getId()) : correlationId;
			var failureEvent = new StudentEvent(correlationId, "STUDENT_CREATION_FAILED", student);

			// 2. Publish the failure event so the Saga can execute compensating
			// transactions
			kafkaTemplate.send("student-events", kafkaKey, failureEvent);
			log.info("Published STUDENT_CREATION_FAILED for Saga rollback. CorrelationId: {}", correlationId);

			// 3. (Optional Industry Standard) Persist to a Dead Letter Database Table here
			// for manual reconciliation or alerting dashboards.
			// deadLetterService.save(commandEvent, originalTopic, errorMessage);

		} catch (Exception e) {
			// If the DLT processing *also* fails (e.g., Kafka is totally down),
			// log a critical alert. This usually requires manual intervention.
			log.error("CRITICAL: Failed to process DLT message for CorrelationId: {}", correlationId, e);
		}
	}
	// --- Reactive Endpoints ---

	@GetMapping("/reactive/getStudents/{id}")
	public Mono<ResponseEntity<Student>> getStudentReactive(@PathVariable int id) {
		return Mono.justOrEmpty(service.getStudent(id)).map(student -> {
			return ResponseEntity.ok(student);
		}).defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@GetMapping("/reactive/getAllStudent")
	public Flux<Student> getAllStudentReactive() {
		return Flux.fromIterable(service.getAllStudent())
				.doOnNext(student -> log.debug("Streaming student: {}", student.getId()));
	}

	public static int divide(int a, int b) {
		if (b == 0)
			throw new ArithmeticException("Cannot divide by zero");
		return a / b;
	}
}