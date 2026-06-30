package se.sundsvall.caremanagement.statushistory.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.statushistory.integration.db.StatusHistoryRepository;
import se.sundsvall.caremanagement.statushistory.integration.db.model.StatusHistoryEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class StatusHistoryServiceTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";

	@Mock
	private ErrandRepository errandRepositoryMock;

	@Mock
	private StatusHistoryRepository repositoryMock;

	@InjectMocks
	private StatusHistoryService service;

	@Test
	void listForErrandReturnsMappedEntries() {
		final var ts1 = FIXED_TIMESTAMP.minusHours(1);
		final var ts2 = FIXED_TIMESTAMP;

		when(errandRepositoryMock.existsByIdAndNamespaceAndMunicipalityId("e1", NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(repositoryMock.findByErrandIdOrderByChangedAtDesc("e1")).thenReturn(List.of(
			StatusHistoryEntity.create().withId("h2").withErrandId("e1").withFromStatus("OPEN").withToStatus("CLOSED").withChangedBy("u").withChangedAt(ts2),
			StatusHistoryEntity.create().withId("h1").withErrandId("e1").withFromStatus(null).withToStatus("OPEN").withChangedBy(null).withChangedAt(ts1)));

		final var result = service.listForErrand(MUNICIPALITY_ID, NAMESPACE, "e1");

		assertThat(result).extracting("id", "errandId", "fromStatus", "toStatus", "changedBy", "changedAt")
			.containsExactly(
				tuple("h2", "e1", "OPEN", "CLOSED", "u", ts2),
				tuple("h1", "e1", null, "OPEN", null, ts1));
		verify(errandRepositoryMock).existsByIdAndNamespaceAndMunicipalityId("e1", NAMESPACE, MUNICIPALITY_ID);
		verify(repositoryMock).findByErrandIdOrderByChangedAtDesc("e1");
	}

	@Test
	void listForErrandReturnsEmptyListWhenNone() {
		when(errandRepositoryMock.existsByIdAndNamespaceAndMunicipalityId("e2", NAMESPACE, MUNICIPALITY_ID)).thenReturn(true);
		when(repositoryMock.findByErrandIdOrderByChangedAtDesc("e2")).thenReturn(List.of());

		assertThat(service.listForErrand(MUNICIPALITY_ID, NAMESPACE, "e2")).isEmpty();
		verify(errandRepositoryMock).existsByIdAndNamespaceAndMunicipalityId("e2", NAMESPACE, MUNICIPALITY_ID);
	}

	@Test
	void listForErrandThrowsNotFoundWhenErrandMissing() {
		when(errandRepositoryMock.existsByIdAndNamespaceAndMunicipalityId("missing", NAMESPACE, MUNICIPALITY_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.listForErrand(MUNICIPALITY_ID, NAMESPACE, "missing"))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessageContaining("No errand with id 'missing' found in namespace 'my-namespace' for municipality id '2281'");

		verify(errandRepositoryMock).existsByIdAndNamespaceAndMunicipalityId("missing", NAMESPACE, MUNICIPALITY_ID);
		verifyNoInteractions(repositoryMock);
	}
}
