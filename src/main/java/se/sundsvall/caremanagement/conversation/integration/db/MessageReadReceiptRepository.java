package se.sundsvall.caremanagement.conversation.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.caremanagement.conversation.integration.db.model.MessageReadReceiptEntity;

@CircuitBreaker(name = "messageReadReceiptRepository")
public interface MessageReadReceiptRepository extends JpaRepository<MessageReadReceiptEntity, String> {

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
