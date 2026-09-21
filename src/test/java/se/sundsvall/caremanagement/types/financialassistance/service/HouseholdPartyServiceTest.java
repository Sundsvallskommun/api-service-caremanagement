package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseholdPartyServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private StakeholderService stakeholderServiceMock;

	@Mock
	private FinancialAssistanceRepository financialAssistanceRepositoryMock;

	@Mock
	private CitizenService citizenServiceMock;

	@InjectMocks
	private HouseholdPartyService service;

	@Test
	void householdFromStakeholdersAndPersons() {
		when(stakeholderServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Stakeholder.create().withRole("APPLICANT").withExternalId("party-a").withFirstName("Anna").withLastName("Andersson"),
			Stakeholder.create().withRole("CO_APPLICANT").withExternalId("party-b")));
		final var applicantPerson = FaPerson.create().withRole("APPLICANT").withPartyId("party-a").withPaymentSameAsPrevious(false);
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withPersons(List.of(applicantPerson))));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, "party-a")).thenReturn(Optional.of("pnr-a"));

		final var household = service.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(household.applicantPersonalNumber()).contains("pnr-a");
		assertThat(household.coApplicantPresent()).isTrue();
		assertThat(household.applicantPerson()).contains(applicantPerson);
		assertThat(household.applicantName()).contains("Anna Andersson");
	}

	@Test
	void householdFallsBackToPersonRowsAndToleratesNothing() {
		when(stakeholderServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(Stakeholder.create().withRole("APPLICANT")));
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withPersons(List.of(
			FaPerson.create().withRole("APPLICANT").withPartyId("party-a")))));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, "party-a")).thenReturn(Optional.empty());

		final var household = service.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(household.applicantPersonalNumber()).isEmpty();
		assertThat(household.coApplicantPresent()).isFalse();
		assertThat(household.applicantName()).isEmpty(); // blank stakeholder name → no display name
	}

	@Test
	void householdWithNoDataAtAll() {
		when(stakeholderServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());

		final var household = service.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(household.applicantPersonalNumber()).isEmpty();
		assertThat(household.coApplicantPresent()).isFalse();
		assertThat(household.applicantPerson()).isEmpty();
		assertThat(household.applicantName()).isEmpty();
		verifyNoInteractions(citizenServiceMock);
	}
}
