package se.sundsvall.caremanagement.core.spi;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrandQueryServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "MY_NAMESPACE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@InjectMocks
	private ErrandQueryService service;

	@Test
	void findErrandMapsEntityToApiModel() {
		final var entity = ErrandEntity.create().withId(ERRAND_ID).withTypeSlug("t").withStatus("OPEN").withAssignedUserId("jane01doe");
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.of(entity));

		final var result = service.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(ERRAND_ID);
		assertThat(result.get().getStatus()).isEqualTo("OPEN");
		assertThat(result.get().getAssignedUserId()).isEqualTo("jane01doe");
	}

	@Test
	void findErrandEmptyWhenNoSuchErrand() {
		when(errandRepositoryMock.findByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID))
			.thenReturn(Optional.empty());

		assertThat(service.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isEmpty();
	}

	@Test
	void existsWithLockReturnsRepositoryResult() {
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);

		assertThat(service.existsWithLock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isTrue();
		verify(errandRepositoryMock).existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void existsWithLockFalseWhenMissing() {
		when(errandRepositoryMock.existsWithLockingByIdAndNamespaceAndMunicipalityId(ERRAND_ID, NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);

		assertThat(service.existsWithLock(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).isFalse();
	}

	@Test
	void findStatusViewsProjectsStatusAndAssignee() {
		final var open = ErrandEntity.create().withStatus("OPEN").withAssignedUserId("jane01doe");
		final var unassigned = ErrandEntity.create().withStatus("CLOSED");
		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(List.of(open, unassigned));

		final var result = service.findStatusViews(MUNICIPALITY_ID, NAMESPACE, "financial-assistance-new",
			OffsetDateTime.parse("2026-05-01T00:00:00Z"), OffsetDateTime.parse("2026-06-01T00:00:00Z"));

		assertThat(result).containsExactly(
			new ErrandStatusView("OPEN", "jane01doe"),
			new ErrandStatusView("CLOSED", null));
	}

	@Test
	void findStatusViewsBuildsPredicatesAcrossOptionalInputs() {
		when(errandRepositoryMock.findAll(ArgumentMatchers.<Specification<ErrandEntity>>any())).thenReturn(List.of());

		final var from = OffsetDateTime.parse("2026-06-01T00:00:00Z");
		final var to = OffsetDateTime.parse("2026-06-30T23:59:59Z");

		// First call exercises the typeSlug + from + to branches; second skips all three.
		service.findStatusViews(MUNICIPALITY_ID, NAMESPACE, "TYPE-1", from, to);
		service.findStatusViews(MUNICIPALITY_ID, NAMESPACE, null, null, null);

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
