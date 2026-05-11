package in.co.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.co.dto.OutboxEvent;
import in.co.dto.Student;
import in.co.dto.StudentEvent;
import in.co.repo.OutboxEventRepository;
import in.co.repo.StudentRepository;
import jakarta.transaction.Transactional;

@Service
public class StudentService {

	private final StudentRepository repository;
	private final OutboxEventRepository outboxRepository;
	private final ObjectMapper objectMapper;

	public StudentService(StudentRepository repository, OutboxEventRepository outboxRepository,
			ObjectMapper objectMapper) {
		this.repository = repository;
		this.outboxRepository = outboxRepository;
		this.objectMapper = objectMapper;
	}

	public Optional<Student> getStudent(int id) {
		return repository.findById(id);
	}

	public List<Student> getAllStudent() {
		return repository.findAll();
	}

	public Student createStudent(Student student) {
		return repository.save(student);
	}

	@Transactional
	public Student createStudentAndOutboxEvent(Student student, String correlationId) {
		student.setCorrelationId(correlationId);
		student.setStatus("COMPLETED");

		Student savedStudent = repository.save(student);

		try {
			StudentEvent successEvent = new StudentEvent(correlationId, "STUDENT_CREATED", savedStudent);
			String payload = objectMapper.writeValueAsString(successEvent);

			OutboxEvent outboxEvent = new OutboxEvent(String.valueOf(savedStudent.getId()), "STUDENT_CREATED", payload);
			outboxEvent.setStatus("PENDING");
			outboxRepository.save(outboxEvent);

			return savedStudent;
		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize Outbox Event", e);
		}
	}
}