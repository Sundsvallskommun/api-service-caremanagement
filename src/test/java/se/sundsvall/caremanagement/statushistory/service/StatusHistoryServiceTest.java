package se.sundsvall.caremanagement.statushistory.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.statushistory.integration.db.StatusHistoryRepository;
import se.sundsvall.caremanagement.statushistory.integration.db.model.StatusHistoryEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusHistoryServiceTest {

	@Mock
	private StatusHistoryRepository repositoryMock;

	@InjectMocks
	private StatusHistoryService service;

	@Test
	void listForErrandReturnsMappedEntries() {
		final var ts1 = OffsetDateTime.now().minusHours(1);
		final var ts2 = OffsetDateTime.now();

		when(repositoryMock.findByErrandIdOrderByChangedAtDesc("e1")).thenReturn(List.of(
			StatusHistoryEntity.create().withId("h2").withErrandId("e1").withFromStatus("OPEN").withToStatus("CLOSED").withChangedBy("u").withChangedAt(ts2),
			StatusHistoryEntity.create().withId("h1").withErrandId("e1").withFromStatus(null).withToStatus("OPEN").withChangedBy(null).withChangedAt(ts1)));

		final var result = service.listForErrand("e1");

		assertThat(result).extracting("id", "errandId", "fromStatus", "toStatus", "changedBy", "changedAt")
			.containsExactly(
				tuple("h2", "e1", "OPEN", "CLOSED", "u", ts2),
				tuple("h1", "e1", null, "OPEN", null, ts1));
		verify(repositoryMock).findByErrandIdOrderByChangedAtDesc("e1");
	}

	@Test
	void listForErrandReturnsEmptyListWhenNone() {
		when(repositoryMock.findByErrandIdOrderByChangedAtDesc("e2")).thenReturn(List.of());
		assertThat(service.listForErrand("e2")).isEmpty();
	}
}
