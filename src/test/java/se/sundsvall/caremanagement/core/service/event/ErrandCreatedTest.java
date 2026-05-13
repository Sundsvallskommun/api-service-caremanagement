package se.sundsvall.caremanagement.core.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrandCreatedTest {

	@Test
	void accessors() {
		final var timestamp = OffsetDateTime.now();
		final var event = new ErrandCreated("e1", "type-slug", "2281", "MY_NAMESPACE",
			"reporter", "assignee", timestamp);

		assertThat(event.errandId()).isEqualTo("e1");
		assertThat(event.typeSlug()).isEqualTo("type-slug");
		assertThat(event.municipalityId()).isEqualTo("2281");
		assertThat(event.namespace()).isEqualTo("MY_NAMESPACE");
		assertThat(event.reporterUserId()).isEqualTo("reporter");
		assertThat(event.assignedUserId()).isEqualTo("assignee");
		assertThat(event.timestamp()).isEqualTo(timestamp);
		assertThat(event).isInstanceOf(ErrandEvent.class);
	}
}
