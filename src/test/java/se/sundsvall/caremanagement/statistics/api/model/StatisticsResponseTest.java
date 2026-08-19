package se.sundsvall.caremanagement.statistics.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsResponseTest {

	@Test
	void testAccessors() {
		final var byStatus = List.of(new StatusCount("NEW", 2L));
		final var byAssignee = List.of(new AssigneeCount("joe01doe", 1L));
		final var response = new StatisticsResponse(3L, byStatus, byAssignee, 1L);

		assertThat(response.total()).isEqualTo(3L);
		assertThat(response.byStatus()).isEqualTo(byStatus);
		assertThat(response.byAssignee()).isEqualTo(byAssignee);
		assertThat(response.unassigned()).isEqualTo(1L);
	}
}
