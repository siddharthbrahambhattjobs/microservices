package in.co.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import in.co.dto.ProcessedCommand;

public interface ProcessedCommandRepository extends JpaRepository<ProcessedCommand, Long> {
    Optional<ProcessedCommand> findByCorrelationId(String correlationId);
}