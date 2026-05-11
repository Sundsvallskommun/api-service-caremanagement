package se.sundsvall.caremanagement.integration.db.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTypeTest {

	@Test
	void containsExpectedValues() {
		assertThat(NotificationType.values()).containsExactly(NotificationType.CREATE, NotificationType.UPDATE, NotificationType.DELETE);
	}

	@Test
	void valueOfRoundtrip() {
		for (final var type : NotificationType.values()) {
			assertThat(NotificationType.valueOf(type.name())).isEqualTo(type);
		}
	}
}
