package se.sundsvall.caremanagement.decisions.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.decisions.integration.db.DecisionRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DecisionErrandDeletedListenerTest {

	@Mock
	private DecisionRepository repositoryMock;

	@InjectMocks
	private DecisionErrandDeletedListener listener;

	@Test
	void deletesAllDecisionsForErrand() {
		listener.on(new ErrandDeleted("e1", "type", "2281", "MY_NAMESPACE", "user", OffsetDateTime.now()));

		verify(repositoryMock).deleteByErrandId("e1");
		verifyNoMoreInteractions(repositoryMock);
	}
}
