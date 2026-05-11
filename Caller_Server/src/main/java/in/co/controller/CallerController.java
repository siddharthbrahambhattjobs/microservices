package in.co.controller;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import in.co.dto.IdempotencyRecord;
import in.co.dto.Student;
import in.co.dto.StudentEvent;
import in.co.service.CallerClient;
import in.co.service.CallerIdempotencyService;
import in.co.service.RedisService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

//CallerController.java — Full corrected constructor

@RestController
@RequestMapping("/caller")
public class CallerController {

	private final CallerClient client;
	private final RedisService redisService;
	private final KafkaTemplate<String, StudentEvent> kafkaTemplate;
	private final WebClient webClient;
	private final CallerIdempotencyService idempotencyService;
	private static final Logger log = LoggerFactory.getLogger(CallerController.class);

	public CallerController(CallerClient client, RedisService redisService,
			KafkaTemplate<String, StudentEvent> kafkaTemplate, CallerIdempotencyService idempotencyService,
			WebClient.Builder webClientBuilder,
			// Reads from application-local.yaml: response.service.url
			@Value("${response.service.url:http://localhost:8154/response}") String responseServiceUrl) {

		this.client = client;
		this.redisService = redisService;
		this.kafkaTemplate = kafkaTemplate;
		this.idempotencyService = idempotencyService;
		// NOW correctly resolved from properties
		this.webClient = webClientBuilder.baseUrl(responseServiceUrl).build();
	}


	@CircuitBreaker(name = "CircuitCreateStudent", fallbackMethod = "CreateStudentFallBack")
	@PostMapping("/create")
	public ResponseEntity<?> createStudent(@RequestHeader("Idempotency-Key") String idempotencyKey,
			@RequestBody @Validated Student request) {

		IdempotencyRecord existing = idempotencyService.find(idempotencyKey);

		if (existing != null) {
			if ("COMPLETED".equals(existing.status())) {
				return ResponseEntity.ok(existing.responseBody());
			}
			if ("PROCESSING".equals(existing.status())) {
				return ResponseEntity.status(HttpStatus.ACCEPTED).body("Request already in progress");
			}
		}

		String correlationId = UUID.randomUUID().toString();
		boolean claimed = idempotencyService.tryStart(idempotencyKey, correlationId, request, Duration.ofHours(24));
		if (!claimed) {
			IdempotencyRecord latest = idempotencyService.find(idempotencyKey);
			return ResponseEntity.status(HttpStatus.ACCEPTED).body("Duplicate request for " + latest.correlationId());
		}

		StudentEvent event = new StudentEvent(correlationId, "CREATE_STUDENT",
				new Student(null, request.name(), request.course(), "PENDING"));

		kafkaTemplate.send("student-create-commands", correlationId, event);

		return ResponseEntity.status(HttpStatus.ACCEPTED).body("Student creation initiated");
	}

	@Retry(name = "CircuitgetStudent", fallbackMethod = "getStudentRetryFallBack")
	@CircuitBreaker(name = "CircuitgetStudent", fallbackMethod = "getStudentFallBack")
	@GetMapping("/getStudent/{id}")
	public ResponseEntity<Student> getStudent(@PathVariable int id) {
		var responseFromRedis = redisService.get("stu_" + id, Student.class);
		if (responseFromRedis != null) {
			log.info("Serving student {} from Redis cache", id);
			return ResponseEntity.ok(responseFromRedis);
		} else {
			ResponseEntity<Student> student = client.getStudent(id);
			if (student != null && student.getBody() != null) {
				redisService.set("stu_" + student.getBody().id(), student.getBody(), 300L);
				log.info("Student {} fetched from Response Service and cached in Redis", id);
				return student;
			} else {
				log.warn("Student with ID {} not found in Redis or Response Service", id);
				return ResponseEntity.notFound().build();
			}
		}
	}

	@GetMapping("/getAllStudent")
	public ResponseEntity<?> getAllStudent() {
		try {
			log.info("Fetching all students from Response Service via Feign...");
			ResponseEntity<List<Student>> studentListResponse = client.getAllStudent();

			if (studentListResponse != null && studentListResponse.getBody() != null) {
				List<Student> students = studentListResponse.getBody();
				log.info("Successfully fetched {} students", students.size());
				// Wrap in a new ResponseEntity to strip out stale Feign response headers
				return ResponseEntity.ok(students);
			} else {
				return ResponseEntity.notFound().build();
			}
		} catch (feign.FeignException e) {
			log.error("Feign Error: Status {}, Body {}", e.status(), e.contentUTF8());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("FEIGN ERROR: " + e.getMessage());
		} catch (Exception e) {
			log.error("System Error: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SYSTEM ERROR: " + e.getMessage());
		}
	}

	@KafkaListener(topics = "student-events", groupId = "caller-service-group")
	public void handleStudentEvents(StudentEvent event) {
		var student = event.student();
		var redisKey = "stu_" + student.id();

		switch (event.eventType()) {
		case "STUDENT_CREATED" -> {
			log.info("Saga Success: Student {} created. Updating Redis status to COMPLETED", student.id());
			var completedStudent = student.withStatus("COMPLETED");
			redisService.set(redisKey, completedStudent, 300L);
		}
		case "STUDENT_CREATION_FAILED" ->
			log.warn("Saga Failed. Compensating transaction correlationId: {}", event.correlationId());
		default -> log.warn("Unknown event type received: {}", event.eventType());
		}
	}

	// --- Fallback Methods ---

	public ResponseEntity<Student> getStudentFallBack(int id, Throwable ex) {
		log.error("CircuitBreaker fallback triggered for student {}. Reason: {}", id, ex.getMessage());

		var responseFromRedis = redisService.get("stu_" + id, Student.class);
		if (responseFromRedis != null) {
			log.info("Serving stale fallback data from Redis for student: {}", id);
			return ResponseEntity.ok(responseFromRedis);
		}

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
	}

	public ResponseEntity<Student> getStudentRetryFallBack(int id, Throwable ex) {
		log.error("Retry fallback triggered for student {}. All retries exhausted. Reason: {}", id, ex.getMessage());
		return getStudentFallBack(id, ex);
	}

	public ResponseEntity<String> createStudentFallBack(Student student, String idempotencyKey, Throwable ex) {
		log.error("CircuitBreaker triggered for createStudent. Reason: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body("The student creation service is temporarily unavailable. Please try again later.");
	}

	// --- Reactive Endpoints ---

	@GetMapping("/reactive/getStudent/{id}")
	public Mono<ResponseEntity<Student>> getStudentReactive(@PathVariable int id) {
		log.info("Initiating reactive WebClient call for student {}", id);

		return webClient.get().uri("/reactive/getStudents/{id}", id).retrieve().toEntity(Student.class)
				.doOnSuccess(response -> log.info("Successfully fetched student {} reactively", id))
				.onErrorResume(e -> {
					log.error("Reactive WebClient Error for student {}: {}", id, e.getMessage());
					return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
				});
	}

	@GetMapping("/reactive/getAllStudent")
	public Flux<Student> getAllStudentReactive() {
		log.info("Initiating reactive WebClient streaming call for all students");

		return webClient.get().uri("/reactive/getAllStudent").retrieve().bodyToFlux(Student.class)
				.doOnNext(student -> log.info("Reactive stream received student: {}", student.id()))
				.onErrorResume(e -> {
					log.error("Reactive WebClient Error during stream: {}", e.getMessage());
					return Flux.empty(); // Gracefully return an empty stream on failure
				});
	}

	@GetMapping("/angular")
	public String responseToAngular() {
		return "response from caller-api";
	}

}