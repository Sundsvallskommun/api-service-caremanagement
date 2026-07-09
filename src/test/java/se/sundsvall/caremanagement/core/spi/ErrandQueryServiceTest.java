package se.sundsvall.caremanagement.core.spi;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;

import static org.assertj.core.api.Assertions.assertThat;
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

}
