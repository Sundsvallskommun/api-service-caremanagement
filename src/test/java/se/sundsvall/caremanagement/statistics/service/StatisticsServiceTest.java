package se.sundsvall.caremanagement.statistics.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.statistics.api.model.AssigneeCount;
import se.sundsvall.caremanagement.statistics.api.model.StatusCount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

	@Mock
	private ErrandRepository errandRepositoryMock;

	@InjectMocks
	private StatisticsService service;

	@Test
	void computeAggregatesCountsPerStatusAndAssignee() {
		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(List.of(
			ErrandEntity.create().withStatus("NEW").withAssignedUserId("user1"),
			ErrandEntity.create().withStatus("NEW"),
			ErrandEntity.create().withStatus("DECIDED").withAssignedUserId("user1")));

		final var result = service.compute("2281", "my-namespace", null, null, null);

		assertThat(result.total()).isEqualTo(3L);
		assertThat(result.unassigned()).isEqualTo(1L);
		assertThat(result.byStatus()).containsExactly(new StatusCount("DECIDED", 1L), new StatusCount("NEW", 2L));
		assertThat(result.byAssignee()).containsExactly(new AssigneeCount("user1", 2L));
	}

	@Test
	void computeTreatsMissingStatusAsUnknown() {
		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(List.of(
			ErrandEntity.create().withAssignedUserId("user1")));

		final var result = service.compute("2281", "my-namespace", "TYPE-1", null, null);

		assertThat(result.total()).isEqualTo(1L);
		assertThat(result.unassigned()).isZero();
		assertThat(result.byStatus()).containsExactly(new StatusCount("UNKNOWN", 1L));
		assertThat(result.byAssignee()).containsExactly(new AssigneeCount("user1", 1L));
	}

	@Test
	void computeReturnsEmptyAggregatesForNoErrands() {
		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(List.of());

		final var result = service.compute("2281", "my-namespace", null, null, null);

		assertThat(result.total()).isZero();
		assertThat(result.unassigned()).isZero();
		assertThat(result.byStatus()).isEmpty();
		assertThat(result.byAssignee()).isEmpty();
	}

	@Test
	void selectionBuildsPredicatesAcrossOptionalInputs() {
		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(List.of());

		final var from = OffsetDateTime.parse("2026-06-01T00:00:00Z");
		final var to = OffsetDateTime.parse("2026-06-30T23:59:59Z");

		// First call exercises the typeSlug + from + to branches; second skips all three.
		service.compute("2281", "my-namespace", "TYPE-1", from, to);
		service.compute("2281", "my-namespace", null, null, null);

		final var captor = ArgumentCaptor.forClass(Specification.class);
		verify(errandRepositoryMock, times(2)).findAll(captor.capture());

		final var root = mock(Root.class);
		final var cb = mock(CriteriaBuilder.class);
		final var path = mock(Path.class);
		final var predicate = mock(Predicate.class);
		lenient().when(root.get(anyString())).thenReturn(path);
		lenient().when(cb.equal(any(), any())).thenReturn(predicate);
		lenient().when(cb.greaterThanOrEqualTo(any(), any(OffsetDateTime.class))).thenReturn(predicate);
		lenient().when(cb.lessThanOrEqualTo(any(), any(OffsetDateTime.class))).thenReturn(predicate);
		lenient().when(cb.and(any(Predicate[].class))).thenReturn(predicate);

		final var specs = captor.getAllValues();
		assertThat(specs.get(0).toPredicate(root, null, cb)).isEqualTo(predicate);
		assertThat(specs.get(1).toPredicate(root, null, cb)).isEqualTo(predicate);
	}
}
