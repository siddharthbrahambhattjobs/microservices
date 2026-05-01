package in.co;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import in.co.controller.ResponseController;
import in.co.dto.Student;
import in.co.dto.StudentEvent;
import in.co.service.IdempotencyService;
import in.co.service.StudentService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResponseController Tests")
class ResponseControllerTest {

	// ------------------------------------------------------------------ mocks
	@Mock
	private StudentService studentService;

	@Mock
	@SuppressWarnings("rawtypes")
	private KafkaTemplate kafkaTemplate;

	@Mock
	private IdempotencyService idempotencyService;

	// ----------------------------------------------------------------- system
	// under test
	@InjectMocks
	private ResponseController controller;

	// ----------------------------------------------------------------- helpers
	private Student buildStudent(int id, String name) {
		Student s = new Student();
		s.setId(id);
		s.setName(name);
		s.setStatus("PENDING");
		return s;
	}

	// ===========================================================================
	// 1. getStudent — parameterized over multiple IDs
	// ===========================================================================

	@ParameterizedTest(name = "getStudent({0}) → 200 OK")
	@ValueSource(ints = { 1, 5, 99, 1000 })
	@DisplayName("getStudent returns 200 when student exists")
	void getStudent_found_returnsOk(int id) {
		Student student = buildStudent(id, "Student-" + id);
		when(studentService.getStudent(id)).thenReturn(Optional.of(student));

		ResponseEntity<Student> response = controller.getStudent(id);

		assertAll(() -> assertEquals(HttpStatus.OK, response.getStatusCode()), () -> assertNotNull(response.getBody()),
				() -> assertEquals(id, response.getBody().getId()));
		verify(studentService, times(1)).getStudent(id);
	}

	@ParameterizedTest(name = "getStudent({0}) → 404 Not Found")
	@ValueSource(ints = { 0, -1, 999, Integer.MAX_VALUE })
	@DisplayName("getStudent returns 404 when student is absent")
	void getStudent_notFound_returns404(int id) {
		when(studentService.getStudent(id)).thenReturn(Optional.empty());

		ResponseEntity<Student> response = controller.getStudent(id);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNull(response.getBody());
		verify(studentService).getStudent(id);
	}

	// ===========================================================================
	// 2. getAllStudent
	// ===========================================================================

	@Test
	@DisplayName("getAllStudent returns 200 with list when students exist")
	void getAllStudent_nonEmptyList_returnsOk() {
		List<Student> students = List.of(buildStudent(1, "Alice"), buildStudent(2, "Bob"));
		when(studentService.getAllStudent()).thenReturn(students);

		ResponseEntity<List<Student>> response = controller.getAllStudent();

		assertAll(() -> assertEquals(HttpStatus.OK, response.getStatusCode()), () -> assertNotNull(response.getBody()),
				() -> assertEquals(2, response.getBody().size()));
	}

	static Stream<Arguments> sagaCommandProvider() {
		return Stream.of(Arguments.of(UUID.randomUUID().toString(), 10, "Charlie"),
				Arguments.of(UUID.randomUUID().toString(), 20, "Diana"),
				Arguments.of(UUID.randomUUID().toString(), 30, "Eve"));
	}

	@ParameterizedTest(name = "SAGA success → student [{1}] {2}")
	@MethodSource("sagaCommandProvider")
	@DisplayName("processCreateStudentCommand publishes STUDENT_CREATED on success")
	@SuppressWarnings("unchecked")
	void processCreateStudentCommand_success_publishesCreatedEvent(String uuid, int studentId, String studentName) {

		ResponseController localController = new ResponseController(studentService, kafkaTemplate, idempotencyService, studentService);

		Student incoming = buildStudent(studentId, studentName);
		StudentEvent command = new StudentEvent(uuid, "CREATE_STUDENT", incoming);

		Student saved = buildStudent(studentId, studentName);
		saved.setStatus("COMPLETED");
		when(studentService.createStudent(incoming)).thenReturn(saved);
		when(idempotencyService.claim(anyString())).thenReturn(true);
		// Act
		localController.processCreateStudentCommand(command);

		// Assert — service called & success event published
		verify(studentService, times(1)).createStudent(incoming);
		verify(kafkaTemplate, times(1)).send(eq("student-events"), eq(String.valueOf(studentId)),
				any(StudentEvent.class));
	}

	@ParameterizedTest(name = "SAGA failure → student [{0}] triggers compensating event")
	@ValueSource(ints = { 11, 22, 33 })
	@DisplayName("processCreateStudentCommand publishes STUDENT_CREATION_FAILED on DB error")
	@SuppressWarnings("unchecked")
	void processCreateStudentCommand_dbFailure_publishesFailureEvent(int studentId) {

		ResponseController localController = new ResponseController(studentService, kafkaTemplate, idempotencyService, studentService);

		Student incoming = buildStudent(studentId, "FailStudent-" + studentId);
		StudentEvent command = new StudentEvent(UUID.randomUUID().toString(), "CREATE_STUDENT", incoming);

		when(studentService.createStudent(any(Student.class))).thenThrow(new RuntimeException("DB connection lost"));
		when(idempotencyService.claim(anyString())).thenReturn(true);
		// Act — must NOT propagate exception to caller
		assertDoesNotThrow(() -> localController.processCreateStudentCommand(command));

		// Assert — failure event published on compensating topic
		verify(kafkaTemplate, times(1)).send(eq("student-events"), eq(String.valueOf(studentId)),
				any(StudentEvent.class));
	}

	@Test
	@DisplayName("processCreateStudentCommand sets status FAILED on DB error")
	@SuppressWarnings("unchecked")
	void processCreateStudentCommand_dbFailure_setsStatusFailed() {

		ResponseController localController = new ResponseController(studentService, kafkaTemplate, idempotencyService, studentService);

		Student incoming = buildStudent(50, "FailureStudent");
		StudentEvent command = new StudentEvent(UUID.randomUUID().toString(), "CREATE_STUDENT", incoming);

		when(studentService.createStudent(any())).thenThrow(new RuntimeException("Timeout"));
		when(idempotencyService.claim(anyString())).thenReturn(true);
		// Capture the published event
		ArgumentCaptor<StudentEvent> eventCaptor = ArgumentCaptor.forClass(StudentEvent.class);
		localController.processCreateStudentCommand(command);

		verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
		StudentEvent published = eventCaptor.getValue();

		assertAll(() -> assertEquals("STUDENT_CREATION_FAILED", published.eventType()),
				() -> assertEquals("FAILED", published.student().getStatus()));
	}

	@Test
	@DisplayName("processCreateStudentCommand sets status COMPLETED on success")
	@SuppressWarnings("unchecked")
	void processCreateStudentCommand_success_setsStatusCompleted() {

		ResponseController localController = new ResponseController(studentService, kafkaTemplate, idempotencyService, studentService);

		Student incoming = buildStudent(60, "SuccessStudent");
		StudentEvent command = new StudentEvent(UUID.randomUUID().toString(), "CREATE_STUDENT", incoming);

		Student saved = buildStudent(60, "SuccessStudent");
		when(studentService.createStudent(any())).thenReturn(saved);
		when(idempotencyService.claim(anyString())).thenReturn(true);
		ArgumentCaptor<StudentEvent> eventCaptor = ArgumentCaptor.forClass(StudentEvent.class);
		localController.processCreateStudentCommand(command);

		verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
		StudentEvent published = eventCaptor.getValue();

		assertAll(() -> assertEquals("STUDENT_CREATED", published.eventType()));
	}

	/**
	 * CSV source: a, b, expected result
	 */

	@ParameterizedTest(name = "divide({0}, {1}) = {2}")
	@CsvSource({ "10,  2,  5", "9,   3,  3", "100, 4, 25", "-6,  2, -3", "0,   5,  0", "7,   1,  7" })
	@DisplayName("divide returns correct quotient for valid inputs")
	void divide_validInputs_returnsCorrectResult(int a, int b, int expected) {
		assertEquals(expected, ResponseController.divide(a, b));
	}

	@ParameterizedTest(name = "divide({0}, 0) → ArithmeticException")
	@ValueSource(ints = { 1, -1, 0, 100, Integer.MIN_VALUE, Integer.MAX_VALUE })
	@DisplayName("divide throws ArithmeticException when divisor is zero")
	void divide_divisionByZero_throwsArithmeticException(int a) {
		ArithmeticException ex = assertThrows(ArithmeticException.class, () -> ResponseController.divide(a, 0));
		assertEquals("Cannot divide by zero", ex.getMessage());
	}

	@Test
	@DisplayName("divide throws ArithmeticException with exact message")
	void divide_byZero_exceptionMessageIsCorrect() {
		ArithmeticException ex = assertThrows(ArithmeticException.class, () -> ResponseController.divide(42, 0));

		assertAll(() -> assertNotNull(ex.getMessage()),
				() -> assertTrue(ex.getMessage().toLowerCase().contains("zero")));
	}

	@Test
	@DisplayName("divide does NOT throw for non-zero denominator")
	void divide_nonZeroDenominator_doesNotThrow() {
		assertDoesNotThrow(() -> ResponseController.divide(50, 5));
	}

	// ===========================================================================
	// 5. Edge / boundary cases
	// ===========================================================================

	@Test
	@DisplayName("getStudent with negative ID returns 404 gracefully")
	void getStudent_negativeId_returns404() {
		when(studentService.getStudent(-1)).thenReturn(Optional.empty());

		ResponseEntity<Student> response = controller.getStudent(-1);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	@DisplayName("getAllStudent with single-element list returns 200")
	void getAllStudent_singleStudent_returnsOk() {
		when(studentService.getAllStudent()).thenReturn(List.of(buildStudent(1, "Solo")));

		ResponseEntity<List<Student>> response = controller.getAllStudent();

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(1, response.getBody().size());
	}
}