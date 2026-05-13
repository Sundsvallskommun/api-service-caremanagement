package se.sundsvall.caremanagement.attachments.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.attachments.integration.db.model.AttachmentDataEntity;

@CircuitBreaker(name = "attachmentDataRepository")
public interface AttachmentDataRepository extends JpaRepository<AttachmentDataEntity, Integer> {
}
