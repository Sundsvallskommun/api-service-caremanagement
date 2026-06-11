package se.sundsvall.caremanagement.conversation.api.model;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void builderMethods() {
		final var id = "m1";
		final var errandId = "e1";
		final var direction = "OUTBOUND";
		final var body = "body";
		final var author = "author";
		final var created = FIXED_TIMESTAMP;

		final var message = Message.create()
			.withId(id)
			.withErrandId(errandId)
			.withDirection(direction)
			.withBody(body)
			.withAuthor(author)
			.withCreated(created);

		assertThat(message.getId()).isEqualTo(id);
		assertThat(message.getErrandId()).isEqualTo(errandId);
		assertThat(message.getDirection()).isEqualTo(direction);
		assertThat(message.getBody()).isEqualTo(body);
		assertThat(message.getAuthor()).isEqualTo(author);
		assertThat(message.getCreated()).isEqualTo(created);
	}

	@Test
	void setters() {
		final var message = Message.create();
		message.setId("id");
		message.setErrandId("eid");
		message.setDirection("INBOUND");
		message.setBody("b");
		message.setAuthor("a");
		final var ts = FIXED_TIMESTAMP;
		message.setCreated(ts);

		assertThat(message.getId()).isEqualTo("id");
		assertThat(message.getErrandId()).isEqualTo("eid");
		assertThat(message.getDirection()).isEqualTo("INBOUND");
		assertThat(message.getBody()).isEqualTo("b");
		assertThat(message.getAuthor()).isEqualTo("a");
		assertThat(message.getCreated()).isEqualTo(ts);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(Message.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsAndHashCode() {
		final var ts = FIXED_TIMESTAMP;
		final var a = Message.create().withId("1").withErrandId("e").withDirection("OUTBOUND").withBody("b").withAuthor("u").withCreated(ts);
		final var b = Message.create().withId("1").withErrandId("e").withDirection("OUTBOUND").withBody("b").withAuthor("u").withCreated(ts);
		final var c = Message.create().withId("2");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
	}
}
