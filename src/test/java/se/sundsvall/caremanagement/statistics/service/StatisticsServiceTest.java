package se.sundsvall.caremanagement.statistics.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.spi.ErrandQueryService;
import se.sundsvall.caremanagement.core.spi.ErrandStatusView;
import se.sundsvall.caremanagement.statistics.api.model.AssigneeCount;
import se.sundsvall.caremanagement.statistics.api.model.StatusCount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

	@Mock
	private ErrandQueryService errandQueryServiceMock;

	@InjectMocks
	private StatisticsService service;

	@Test
	void computeAggregatesCountsPerStatusAndAssignee() {
		when(errandQueryServiceMock.findStatusViews(any(), any(), any(), any(), any())).thenReturn(List.of(
			new ErrandStatusView("NEW", "user1"),
			new ErrandStatusView("NEW", null),
			new ErrandStatusView("DECIDED", "user1")));

		final var result = service.compute("2281", "my-namespace", null, null, null);

		assertThat(result.total()).isEqualTo(3L);
		assertThat(result.unassigned()).isEqualTo(1L);
		assertThat(result.byStatus()).containsExactly(new StatusCount("DECIDED", 1L), new StatusCount("NEW", 2L));
		assertThat(result.byAssignee()).containsExactly(new AssigneeCount("user1", 2L));
	}

	@Test
	void computeTreatsMissingStatusAsUnknown() {
		when(errandQueryServiceMock.findStatusViews(any(), any(), any(), any(), any())).thenReturn(List.of(
			new ErrandStatusView(null, "user1")));

		final var result = service.compute("2281", "my-namespace", "TYPE-1", null, null);

		assertThat(result.total()).isEqualTo(1L);
		assertThat(result.unassigned()).isZero();
		assertThat(result.byStatus()).containsExactly(new StatusCount("UNKNOWN", 1L));
		assertThat(result.byAssignee()).containsExactly(new AssigneeCount("user1", 1L));
	}

	@Test
	void computeReturnsEmptyAggregatesForNoErrands() {
		when(errandQueryServiceMock.findStatusViews(any(), any(), any(), any(), any())).thenReturn(List.of());

		final var result = service.compute("2281", "my-namespace", null, null, null);

		assertThat(result.total()).isZero();
		assertThat(result.unassigned()).isZero();
		assertThat(result.byStatus()).isEmpty();
		assertThat(result.byAssignee()).isEmpty();
	}
}
