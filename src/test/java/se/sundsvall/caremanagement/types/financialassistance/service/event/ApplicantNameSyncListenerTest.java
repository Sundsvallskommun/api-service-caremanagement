package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.core.spi.ErrandQueryService;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.stakeholders.service.event.StakeholderMutated;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicantNameSyncListenerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "11111111-1111-1111-1111-111111111111";

	@Mock
	private ErrandQueryService errandQueryServiceMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private StakeholderService stakeholderServiceMock;

	@InjectMocks
	private ApplicantNameSyncListener listener;

	private static StakeholderMutated event() {
		return new StakeholderMutated(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	private void errandHasApplicantName(final String current) {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.of(Errand.create().withApplicantName(current)));
	}

	@Test
	void writesPersonNameFromApplicantStakeholder() {
		errandHasApplicantName(null);
		when(stakeholderServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Stakeholder.create().withRole("CO_APPLICANT").withFirstName("Bo").withLastName("Bergström"),
			Stakeholder.create().withRole("APPLICANT").withFirstName("Anna").withLastName("Andersson")));

		listener.syncApplicantName(event());

		verify(errandServiceMock).updateApplicantName(ERRAND_ID, "Anna Andersson");
	}

	@Test
	void prefersOrganizationNameWhenPresent() {
		errandHasApplicantName(null);
		when(stakeholderServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Stakeholder.create().withRole("APPLICANT").withFirstName("ignored").withOrganizationName("Acme AB")));

		listener.syncApplicantName(event());

		verify(errandServiceMock).updateApplicantName(ERRAND_ID, "Acme AB");
	}

	@Test
	void skipsUpdateWhenNameUnchanged() {
		errandHasApplicantName("Anna Andersson");
		when(stakeholderServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Stakeholder.create().withRole("APPLICANT").withFirstName("Anna").withLastName("Andersson")));

		listener.syncApplicantName(event());

		verify(errandServiceMock, never()).updateApplicantName(any(), any());
	}

	@Test
	void clearsNameWhenApplicantRemoved() {
		errandHasApplicantName("Anna Andersson");
		when(stakeholderServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Stakeholder.create().withRole("CO_APPLICANT").withFirstName("Bo").withLastName("Bergström")));

		listener.syncApplicantName(event());

		verify(errandServiceMock).updateApplicantName(ERRAND_ID, null);
	}

	@Test
	void skipsEntirelyWhenErrandNotFound() {
		when(errandQueryServiceMock.findErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Optional.empty());

		listener.syncApplicantName(event());

		verifyNoInteractions(stakeholderServiceMock);
		verify(errandServiceMock, never()).updateApplicantName(any(), any());
	}
}
