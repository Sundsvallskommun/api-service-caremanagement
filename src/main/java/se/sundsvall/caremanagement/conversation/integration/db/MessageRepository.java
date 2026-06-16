package se.sundsvall.caremanagement.conversation.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageEntity;

@CircuitBreaker(name = "messageRepository")
public interface MessageRepository extends JpaRepository<MessageEntity, String> {

	List<MessageEntity> findByErrandIdOrderByCreatedAsc(String errandId);

	Optional<MessageEntity> findByIdAndErrandId(String id, String errandId);
}
