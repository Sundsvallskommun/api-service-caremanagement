package se.sundsvall.caremanagement.permit.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.permit.integration.db.PermitRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PermitErrandDeletedListenerTest {

	@Mock
	private PermitRepository repositoryMock;

	@InjectMocks
	private PermitErrandDeletedListener listener;

	@Test
	void onErrandDeletedRemovesPermits() {
		listener.deletePermitsForErrand(new ErrandDeleted("errand-1", "TYPE-1", "2281", "my-namespace", "user", OffsetDateTime.parse("2026-06-03T10:00:00Z")));

		verify(repositoryMock).deleteByErrandId("errand-1");
		verifyNoMoreInteractions(repositoryMock);
	}
}
