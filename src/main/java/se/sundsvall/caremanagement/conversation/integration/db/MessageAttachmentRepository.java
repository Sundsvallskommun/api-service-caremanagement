package se.sundsvall.caremanagement.conversation.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentEntity;

@CircuitBreaker(name = "messageAttachmentRepository")
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachmentEntity, String> {

	List<MessageAttachmentEntity> findByMessageId(String messageId);

	List<MessageAttachmentEntity> findByMessageIdIn(List<String> messageIds);

	Optional<MessageAttachmentEntity> findByMessageIdAndId(String messageId, String id);
}
