package se.sundsvall.caremanagement.stakeholders.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.stakeholders.integration.db.StakeholderRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class StakeholderErrandDeletedListenerTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@Mock
	private StakeholderRepository repositoryMock;

	@InjectMocks
	private StakeholderErrandDeletedListener listener;

	@Test
	void deletesAllStakeholdersForErrand() {
		listener.deleteStakeholdersForErrand(new ErrandDeleted("e1", "type", "2281", "MY_NAMESPACE", "user", FIXED_TIMESTAMP));

		verify(repositoryMock).deleteByErrandId("e1");
		verifyNoMoreInteractions(repositoryMock);
	}
}
