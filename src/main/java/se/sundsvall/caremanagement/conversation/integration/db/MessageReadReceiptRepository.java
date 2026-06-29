package se.sundsvall.caremanagement.conversation.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageReadReceiptEntity;

@CircuitBreaker(name = "messageReadReceiptRepository")
public interface MessageReadReceiptRepository extends JpaRepository<MessageReadReceiptEntity, String> {

	/**
	 * Idempotently records one read receipt. {@code INSERT IGNORE} turns the {@code (message_id, reader_side)} unique
	 * collision into a no-op warning instead of a {@code DataIntegrityViolationException}, so two concurrent
	 * mark-as-read calls for the same message+side don't fail (nor poison the transaction's other inserts).
	 * {@code readAt} is bound as a UTC {@link LocalDateTime} to match the entity's {@code NORMALIZE} timezone storage.
	 */
	@Modifying
	@Query(value = """
		insert ignore into message_read_receipt (id, message_id, reader_side, read_by, read_at)
		values (:id, :messageId, :readerSide, :readBy, :readAt)
		""", nativeQuery = true)
	void insertIgnore(@Param("id") String id, @Param("messageId") String messageId, @Param("readerSide") String readerSide, @Param("readBy") String readBy, @Param("readAt") LocalDateTime readAt);

	/**
	 * Counts the messages on the errand with the given {@code direction} (the ones addressed to a reader side) that have
	 * no read receipt for that side yet — i.e. that side's unread count.
	 */
	@Query("""
		select count(m) from MessageEntity m
		where m.errandId = :errandId
		  and m.direction = :direction
		  and not exists (
		    select 1 from MessageReadReceiptEntity r
		    where r.messageId = m.id and r.readerSide = :readerSide
		  )
		""")
	long countUnread(@Param("errandId") String errandId, @Param("direction") String direction, @Param("readerSide") String readerSide);

	/** The message ids among {@code messageIds} that already have a receipt for the given side. */
	@Query("""
		select r.messageId from MessageReadReceiptEntity r
		where r.readerSide = :readerSide and r.messageId in :messageIds
		""")
	List<String> findReadMessageIds(@Param("readerSide") String readerSide, @Param("messageIds") List<String> messageIds);
}
