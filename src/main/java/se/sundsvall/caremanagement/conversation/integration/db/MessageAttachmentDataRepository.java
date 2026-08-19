package se.sundsvall.caremanagement.conversation.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentDataEntity;

@CircuitBreaker(name = "messageAttachmentDataRepository")
public interface MessageAttachmentDataRepository extends JpaRepository<MessageAttachmentDataEntity, Integer> {

	Optional<MessageAttachmentDataEntity> findByMessageAttachmentId(String messageAttachmentId);
}
