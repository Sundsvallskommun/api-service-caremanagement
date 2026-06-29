package se.sundsvall.caremanagement.conversation.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageAttachmentEntity;

@CircuitBreaker(name = "messageAttachmentRepository")
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachmentEntity, String> {

	List<MessageAttachmentEntity> findByMessageId(String messageId);

	List<MessageAttachmentEntity> findByMessageIdIn(List<String> messageIds);

	Optional<MessageAttachmentEntity> findByMessageIdAndId(String messageId, String id);

	/**
	 * Every attachment on the errand's conversation, oldest first. {@code message_attachment} has no errand column, so
	 * this joins through {@code errand_message} on the (unmapped) {@code messageId}.
	 */
	@Query("""
		select a from MessageAttachmentEntity a
		join MessageEntity m on m.id = a.messageId
		where m.errandId = :errandId
		order by a.created asc
		""")
	List<MessageAttachmentEntity> findByErrandIdOrderByCreatedAsc(@Param("errandId") String errandId);
}
