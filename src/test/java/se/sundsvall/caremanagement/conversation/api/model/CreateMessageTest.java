package se.sundsvall.caremanagement.conversation.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateMessageTest {

	@Test
	void testAccessors() {
		final var message = new CreateMessage("OUTBOUND", "message body", "joe01doe", "f47ac10b-58cc-4372-a567-0e02b2c3d479");

		assertThat(message.direction()).isEqualTo("OUTBOUND");
		assertThat(message.body()).isEqualTo("message body");
		assertThat(message.author()).isEqualTo("joe01doe");
		assertThat(message.inReplyToId()).isEqualTo("f47ac10b-58cc-4372-a567-0e02b2c3d479");
	}

	@Test
	void testAuthorAndInReplyToAreOptional() {
		final var message = new CreateMessage("INBOUND", "message body", null, null);

		assertThat(message.direction()).isEqualTo("INBOUND");
		assertThat(message.body()).isEqualTo("message body");
		assertThat(message.author()).isNull();
		assertThat(message.inReplyToId()).isNull();
	}
}
