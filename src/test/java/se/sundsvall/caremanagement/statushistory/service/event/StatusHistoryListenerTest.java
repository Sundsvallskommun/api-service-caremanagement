package se.sundsvall.caremanagement.statushistory.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.event.ErrandStatusChanged;
import se.sundsvall.caremanagement.statushistory.integration.db.StatusHistoryRepository;
import se.sundsvall.caremanagement.statushistory.integration.db.model.StatusHistoryEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatusHistoryListenerTest {

	private static final OffsetDateTime TS = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Mock
	private StatusHistoryRepository statusHistoryRepositoryMock;

	@InjectMocks
	private StatusHistoryListener listener;

	@Test
	void persistsStatusChangeAsHistoryRow() {
		listener.on(new ErrandStatusChanged("errand-1", "financial-assistance", "2281", "EB", "RECEIVED", "AWAITING_DECISION", "carola01winberg", TS));

		final var captor = ArgumentCaptor.forClass(StatusHistoryEntity.class);
		verify(statusHistoryRepositoryMock).save(captor.capture());

		final var entity = captor.getValue();
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getFromStatus()).isEqualTo("RECEIVED");
		assertThat(entity.getToStatus()).isEqualTo("AWAITING_DECISION");
		assertThat(entity.getChangedBy()).isEqualTo("carola01winberg");
		assertThat(entity.getChangedAt()).isEqualTo(TS);
	}
}
