package se.sundsvall.caremanagement.referral.service.event;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.referral.integration.db.ReferralRepository;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ReferralErrandDeletedListenerTest {

	@Mock
	private ReferralRepository repositoryMock;

	@InjectMocks
	private ReferralErrandDeletedListener listener;

	@Test
	void onErrandDeletedRemovesReferrals() {
		listener.deleteReferralsForErrand(new ErrandDeleted("errand-1", "TYPE-1", "2281", "my-namespace", "user", OffsetDateTime.parse("2026-06-03T10:00:00Z")));

		verify(repositoryMock).deleteByErrandId("errand-1");
		verifyNoMoreInteractions(repositoryMock);
	}
}
