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

class MessageEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(MessageEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "m1";
		final var errandId = "e1";
		final var direction = "OUTBOUND";
		final var body = "body";
		final var author = "author";
		final var inReplyToId = "in-reply-to-1";
		final var created = FIXED_TIMESTAMP;

		final var entity = MessageEntity.create()
			.withId(id)
			.withErrandId(errandId)
			.withDirection(direction)
			.withBody(body)
			.withAuthor(author)
			.withInReplyToId(inReplyToId)
			.withCreated(created);

		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getErrandId()).isEqualTo(errandId);
		assertThat(entity.getDirection()).isEqualTo(direction);
		assertThat(entity.getBody()).isEqualTo(body);
		assertThat(entity.getAuthor()).isEqualTo(author);
		assertThat(entity.getInReplyToId()).isEqualTo(inReplyToId);
		assertThat(entity.getCreated()).isEqualTo(created);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MessageEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new MessageEntity()).hasAllNullFieldsOrProperties();
	}
}
