package in.co.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import in.co.dto.Student;
import in.co.repo.StudentRepository;
import in.co.service.StudentService;

/**
 * Integration test — loads the real Spring context with H2 in-memory DB.
 * StudentService → StudentRepository → H2 (no mocking anywhere).
 *
 * Requires in src/test/resources:
 *   application-test.properties  (provided alongside this file)
 */
@SpringBootTest
@ActiveProfiles("test")
class StudentServiceIntegrationTest {

    @Autowired
    private StudentService service;

    @Autowired
    private StudentRepository repository;

    private Student saved;

    @BeforeEach
    void setUp() {
        // Persist one student before each test
        Student s = new Student();
        s.setName("Alice");
        s.setStatus("ACTIVE");
        saved = repository.save(s);
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // getStudent
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getStudent – returns student that actually exists in DB")
    void getStudent_found() {
        Optional<Student> result = service.getStudent(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice");
        assertThat(result.get().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("getStudent – returns empty when id does not exist in DB")
    void getStudent_notFound() {
        Optional<Student> result = service.getStudent(Integer.MAX_VALUE);

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // getAllStudent
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getAllStudent – returns all students persisted in DB")
    void getAllStudent_returnsAll() {
        Student second = new Student();
        second.setName("Bob");
        second.setStatus("INACTIVE");
        repository.save(second);

        List<Student> result = service.getAllStudent();

        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result).extracting(Student::getName).contains("Alice", "Bob");
    }

    @Test
    @DisplayName("getAllStudent – returns empty list after all records deleted")
    void getAllStudent_emptyAfterDelete() {
        repository.deleteAll();

        List<Student> result = service.getAllStudent();

        assertThat(result).isEmpty();
    }

    // -----------------------------------------------------------------------
    // createStudent
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createStudent – persists student and auto-generates id")
    void createStudent_persistsAndReturnsWithId() {
        Student newStudent = new Student();
        newStudent.setName("Charlie");
        newStudent.setStatus("PENDING");

        Student created = service.createStudent(newStudent);

        assertThat(created.getId()).isPositive();
        // Verify it's actually in the DB
        assertThat(repository.findById(created.getId())).isPresent();
    }

    @Test
    @DisplayName("createStudent – updates status when student already exists")
    void createStudent_updatesExistingRecord() {
        saved.setStatus("COMPLETED");

        Student updated = service.createStudent(saved);

        assertThat(updated.getStatus()).isEqualTo("COMPLETED");
        assertThat(repository.findById(saved.getId()).get().getStatus())
                .isEqualTo("COMPLETED");
    }
}