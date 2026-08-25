package se.sundsvall.caremanagement.conversation.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkMessagesReadTest {

	@Test
	void testAccessor() {
		final var messageIds = List.of("f47ac10b-58cc-4372-a567-0e02b2c3d479", "a1b2c3d4-e5f6-7890-abcd-ef0123456789");

		final var request = new MarkMessagesRead(messageIds);

		assertThat(request.messageIds()).isEqualTo(messageIds);
	}
}
