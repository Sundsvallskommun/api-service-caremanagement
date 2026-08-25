package se.sundsvall.caremanagement.conversation.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class MessageReadReceiptEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(MessageReadReceiptEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "r1";
		final var messageId = "m1";
		final var readerSide = "CASEWORKER";
		final var readBy = "joe001doe";
		final var readAt = FIXED_TIMESTAMP;

		final var entity = MessageReadReceiptEntity.create()
			.withId(id)
			.withMessageId(messageId)
			.withReaderSide(readerSide)
			.withReadBy(readBy)
			.withReadAt(readAt);

		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMessageId()).isEqualTo(messageId);
		assertThat(entity.getReaderSide()).isEqualTo(readerSide);
		assertThat(entity.getReadBy()).isEqualTo(readBy);
		assertThat(entity.getReadAt()).isEqualTo(readAt);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MessageReadReceiptEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new MessageReadReceiptEntity()).hasAllNullFieldsOrProperties();
	}
}
