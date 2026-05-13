package se.sundsvall.caremanagement.core.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrandAssignedTest {

	@Test
	void accessors() {
		final var timestamp = OffsetDateTime.now();
		final var event = new ErrandAssigned("e1", "type-slug", "2281", "MY_NAMESPACE",
			"old-user", "new-user", "by-user", timestamp);

		assertThat(event.errandId()).isEqualTo("e1");
		assertThat(event.typeSlug()).isEqualTo("type-slug");
		assertThat(event.municipalityId()).isEqualTo("2281");
		assertThat(event.namespace()).isEqualTo("MY_NAMESPACE");
		assertThat(event.previousAssignee()).isEqualTo("old-user");
		assertThat(event.newAssignee()).isEqualTo("new-user");
		assertThat(event.changedBy()).isEqualTo("by-user");
		assertThat(event.timestamp()).isEqualTo(timestamp);
	}

	@Test
	void implementsErrandEvent() {
		final var event = new ErrandAssigned("e", "t", "m", "n", null, "x", null, OffsetDateTime.now());
		assertThat(event).isInstanceOf(ErrandEvent.class);
	}
}
