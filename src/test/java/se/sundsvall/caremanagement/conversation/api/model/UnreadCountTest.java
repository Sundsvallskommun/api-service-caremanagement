package se.sundsvall.caremanagement.conversation.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnreadCountTest {

	@Test
	void accessor() {
		final var unreadCount = new UnreadCount(3);

		assertThat(unreadCount.unreadCount()).isEqualTo(3);
	}
}
