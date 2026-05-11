package se.sundsvall.caremanagement.integration.db.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSubTypeTest {

	@Test
	void containsExpectedValues() {
		assertThat(NotificationSubType.values()).containsExactly(
			NotificationSubType.ERRAND,
			NotificationSubType.DECISION,
			NotificationSubType.ATTACHMENT,
			NotificationSubType.STAKEHOLDER,
			NotificationSubType.PARAMETER,
			NotificationSubType.SYSTEM);
	}

	@Test
	void valueOfRoundtrip() {
		for (final var sub : NotificationSubType.values()) {
			assertThat(NotificationSubType.valueOf(sub.name())).isEqualTo(sub);
		}
	}
}
