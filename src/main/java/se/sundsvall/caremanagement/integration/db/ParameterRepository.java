package se.sundsvall.caremanagement.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.integration.db.model.ParameterEntity;

@CircuitBreaker(name = "parameterRepository")
public interface ParameterRepository extends JpaRepository<ParameterEntity, String> {
}
