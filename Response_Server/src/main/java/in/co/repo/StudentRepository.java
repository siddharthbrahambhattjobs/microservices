package in.co.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import in.co.dto.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer>  {
	Optional<Student> findByCorrelationId(String correlationId);
}
