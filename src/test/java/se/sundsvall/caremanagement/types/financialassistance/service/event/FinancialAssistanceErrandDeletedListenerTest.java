package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceErrandDeletedListenerTest {

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@InjectMocks
	private FinancialAssistanceErrandDeletedListener listener;

	private static final ErrandDeleted EVENT = new ErrandDeleted(
		"errand-1", "financial-assistance", "2281", "FINANCIAL_ASSISTANCE", "joe01doe",
		OffsetDateTime.parse("2026-06-05T12:00:00Z"));

	@Test
	void deletesWhenPresent() {
		final var entity = FinancialAssistanceEntity.create().withErrandId("errand-1");
		when(repositoryMock.findByErrandId("errand-1")).thenReturn(Optional.of(entity));

		listener.on(EVENT);

		verify(repositoryMock).findByErrandId("errand-1");
		verify(repositoryMock).delete(entity);
		verifyNoMoreInteractions(repositoryMock);
	}

	@Test
	void noopWhenAbsent() {
		when(repositoryMock.findByErrandId("errand-1")).thenReturn(Optional.empty());

		listener.on(EVENT);

		verify(repositoryMock).findByErrandId("errand-1");
		verify(repositoryMock, never()).delete(any());
		verifyNoMoreInteractions(repositoryMock);
	}
}
