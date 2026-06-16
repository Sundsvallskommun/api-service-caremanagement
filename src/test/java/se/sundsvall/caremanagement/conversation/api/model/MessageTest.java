package se.sundsvall.caremanagement.conversation.api.model;

import java.time.OffsetDateTime;
import java.util.List;
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
		final var inReplyToId = "in-reply-to-1";
		final var created = FIXED_TIMESTAMP;
		final var attachments = List.of(MessageAttachment.create().withId("a1").withFileName("f.pdf"));

		final var message = Message.create()
			.withId(id)
			.withErrandId(errandId)
			.withDirection(direction)
			.withBody(body)
			.withAuthor(author)
			.withInReplyToId(inReplyToId)
			.withCreated(created)
			.withAttachments(attachments);

		assertThat(message.getId()).isEqualTo(id);
		assertThat(message.getErrandId()).isEqualTo(errandId);
		assertThat(message.getDirection()).isEqualTo(direction);
		assertThat(message.getBody()).isEqualTo(body);
		assertThat(message.getAuthor()).isEqualTo(author);
		assertThat(message.getInReplyToId()).isEqualTo(inReplyToId);
		assertThat(message.getCreated()).isEqualTo(created);
		assertThat(message.getAttachments()).isEqualTo(attachments);
	}

	@Test
	void setters() {
		final var message = Message.create();
		message.setId("id");
		message.setErrandId("eid");
		message.setDirection("INBOUND");
		message.setBody("b");
		message.setAuthor("a");
		message.setInReplyToId("r1");
		final var ts = FIXED_TIMESTAMP;
		message.setCreated(ts);
		final var attachments = List.of(MessageAttachment.create().withId("a1"));
		message.setAttachments(attachments);

		assertThat(message.getId()).isEqualTo("id");
		assertThat(message.getErrandId()).isEqualTo("eid");
		assertThat(message.getDirection()).isEqualTo("INBOUND");
		assertThat(message.getBody()).isEqualTo("b");
		assertThat(message.getAuthor()).isEqualTo("a");
		assertThat(message.getInReplyToId()).isEqualTo("r1");
		assertThat(message.getCreated()).isEqualTo(ts);
		assertThat(message.getAttachments()).isEqualTo(attachments);
	}

	@Test
	void createReturnsBlankInstanceWithEmptyAttachments() {
		final var message = Message.create();
		assertThat(message).hasAllNullFieldsOrPropertiesExcept("attachments");
		assertThat(message.getAttachments()).isEmpty();
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
