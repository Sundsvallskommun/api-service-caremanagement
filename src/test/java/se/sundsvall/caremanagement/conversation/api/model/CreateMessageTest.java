package se.sundsvall.caremanagement.conversation.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateMessageTest {

	@Test
	void accessors() {
		final var message = new CreateMessage("OUTBOUND", "message body", "joe01doe");

		assertThat(message.direction()).isEqualTo("OUTBOUND");
		assertThat(message.body()).isEqualTo("message body");
		assertThat(message.author()).isEqualTo("joe01doe");
	}

	@Test
	void authorIsOptional() {
		final var message = new CreateMessage("INBOUND", "message body", null);

		assertThat(message.direction()).isEqualTo("INBOUND");
		assertThat(message.body()).isEqualTo("message body");
		assertThat(message.author()).isNull();
	}
}
