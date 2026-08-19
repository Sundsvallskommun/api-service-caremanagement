package se.sundsvall.caremanagement.statistics.api.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssigneeCountTest {

	@Test
	void testAccessors() {
		final var assigneeCount = new AssigneeCount("joe01doe", 5L);

		assertThat(assigneeCount.assignedUserId()).isEqualTo("joe01doe");
		assertThat(assigneeCount.count()).isEqualTo(5L);
	}
}
