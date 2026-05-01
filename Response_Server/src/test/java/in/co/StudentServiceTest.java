package in.co;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import in.co.dto.Student;
import in.co.repo.StudentRepository;
import in.co.service.StudentService;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

	@Mock
	private StudentRepository repository;

	@InjectMocks
	private StudentService service;

	private Student student;

	@BeforeEach
	void setUp() {
		student = new Student();
		student.setId(1);
		student.setName("Alice");
		student.setStatus("ACTIVE");
	}

	// -----------------------------------------------------------------------
	// getStudent
	// -----------------------------------------------------------------------

	@Test
	@DisplayName("getStudent – returns student when found in repository")
	void getStudent_found() {
		given(repository.findById(1)).willReturn(Optional.of(student));

		Optional<Student> result = service.getStudent(1);

		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(1);
		assertThat(result.get().getName()).isEqualTo("Alice");
		verify(repository).findById(1);
	}

	@Test
	@DisplayName("getStudent – returns empty Optional when student not found")
	void getStudent_notFound() {
		given(repository.findById(99)).willReturn(Optional.empty());

		Optional<Student> result = service.getStudent(99);

		assertThat(result).isEmpty();
		verify(repository).findById(99);
	}

	// -----------------------------------------------------------------------
	// getAllStudent
	// -----------------------------------------------------------------------

	@Test
	@DisplayName("getAllStudent – returns populated list when students exist")
	void getAllStudent_nonEmpty() {
		Student second = new Student();
		second.setId(2);
		second.setName("Bob");
		given(repository.findAll()).willReturn(List.of(student, second));

		List<Student> result = service.getAllStudent();

		assertThat(result).hasSize(2);
		assertThat(result).extracting(Student::getName).containsExactly("Alice", "Bob");
		verify(repository).findAll();
	}

	@Test
	@DisplayName("getAllStudent – returns empty list when no students exist")
	void getAllStudent_empty() {
		given(repository.findAll()).willReturn(Collections.emptyList());

		List<Student> result = service.getAllStudent();

		assertThat(result).isEmpty();
		verify(repository).findAll();
	}

	// -----------------------------------------------------------------------
	// createStudent
	// -----------------------------------------------------------------------

	@Test
	@DisplayName("createStudent – delegates save to repository and returns persisted student")
	void createStudent_success() {
		given(repository.save(any(Student.class))).willReturn(student);

		Student result = service.createStudent(student);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1);
		assertThat(result.getName()).isEqualTo("Alice");
		verify(repository).save(student);
	}

	@Test
	@DisplayName("createStudent – does not call findById during save")
	void createStudent_doesNotQueryForExistingStudent() {
		given(repository.save(any(Student.class))).willReturn(student);

		service.createStudent(student);

		verify(repository, never()).findById(any());
	}
}
