package in.co;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import in.co.dto.Student;
import in.co.repo.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentRepositoryTest {

    @Mock
    private StudentRepository repository;

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1);
        student.setName("Alice");
        student.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("findById – returns student when present")
    void findById_returnsStudent() {
        given(repository.findById(1)).willReturn(Optional.of(student));

        Optional<Student> result = repository.findById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice");
        assertThat(result.get().getStatus()).isEqualTo("ACTIVE");
        verify(repository).findById(1);
    }

    @Test
    @DisplayName("findById – returns empty Optional when student not found")
    void findById_notFound() {
        given(repository.findById(99)).willReturn(Optional.empty());

        Optional<Student> result = repository.findById(99);

        assertThat(result).isEmpty();
        verify(repository).findById(99);
    }

    // -----------------------------------------------------------------------
    // findAll
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("findAll – returns list of all students")
    void findAll_returnsList() {
        Student second = new Student();
        second.setId(2);
        second.setName("Bob");
        second.setStatus("INACTIVE");

        given(repository.findAll()).willReturn(List.of(student, second));

        List<Student> result = repository.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Student::getName).containsExactly("Alice", "Bob");
        verify(repository).findAll();
    }

    @Test
    @DisplayName("findAll – returns empty list when no students exist")
    void findAll_emptyList() {
        given(repository.findAll()).willReturn(Collections.emptyList());

        List<Student> result = repository.findAll();

        assertThat(result).isEmpty();
        verify(repository).findAll();
    }

    // -----------------------------------------------------------------------
    // save
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("save – returns persisted student with id assigned")
    void save_returnsPersistedStudent() {
        given(repository.save(any(Student.class))).willReturn(student);

        Student result = repository.save(student);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getName()).isEqualTo("Alice");
        verify(repository).save(student);
    }

    @Test
    @DisplayName("save – updates and returns student with new status")
    void save_updatesExistingStudent() {
        Student updated = new Student();
        updated.setId(1);
        updated.setName("Alice");
        updated.setStatus("COMPLETED");

        given(repository.save(any(Student.class))).willReturn(updated);

        Student result = repository.save(updated);

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(repository).save(updated);
    }

    // -----------------------------------------------------------------------
    // deleteById
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteById – invokes repository delete with correct id")
    void deleteById_callsRepository() {
        repository.deleteById(1);
        verify(repository).deleteById(1);
    }
}
