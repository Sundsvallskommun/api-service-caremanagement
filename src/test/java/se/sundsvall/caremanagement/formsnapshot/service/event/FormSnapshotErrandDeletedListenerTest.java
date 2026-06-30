package se.sundsvall.caremanagement.formsnapshot.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.formsnapshot.integration.db.FormSnapshotRepository;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FormSnapshotErrandDeletedListenerTest {

	@Mock
	private FormSnapshotRepository repositoryMock;

	@InjectMocks
	private FormSnapshotErrandDeletedListener listener;

	@Test
	void onErrandDeletedRemovesSnapshot() {
		listener.on(new ErrandDeleted("errand-1", "TYPE-1", "2281", "my-namespace", "user", OffsetDateTime.parse("2026-06-03T10:00:00Z")));

		verify(repositoryMock).deleteByErrandId("errand-1");
	}
}
