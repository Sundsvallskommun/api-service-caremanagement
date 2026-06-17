package se.sundsvall.caremanagement.conversation.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessagePostedTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Test
	void accessors() {
		final var timestamp = FIXED_TIMESTAMP;
		final var event = new MessagePosted("message-1", "2281", "my-namespace", "errand-1", "OUTBOUND", "author-1", true, timestamp);

		assertThat(event.messageId()).isEqualTo("message-1");
		assertThat(event.municipalityId()).isEqualTo("2281");
		assertThat(event.namespace()).isEqualTo("my-namespace");
		assertThat(event.errandId()).isEqualTo("errand-1");
		assertThat(event.direction()).isEqualTo("OUTBOUND");
		assertThat(event.author()).isEqualTo("author-1");
		assertThat(event.hasAttachments()).isTrue();
		assertThat(event.timestamp()).isEqualTo(timestamp);
	}
}
