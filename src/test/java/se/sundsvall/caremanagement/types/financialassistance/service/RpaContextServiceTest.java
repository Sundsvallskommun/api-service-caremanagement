package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class RpaContextServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String ERRAND_NUMBER = "EB-2026-000123";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private StakeholderService stakeholderServiceMock;

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@Mock
	private CitizenService citizenServiceMock;

	@InjectMocks
	private RpaContextService service;

	@Test
	void resolvesHouseholdPersonalNumbersFromApplicationPersons() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID).withErrandNumber(ERRAND_NUMBER));
		when(stakeholderServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create()
			.withPersons(List.of(
				FaPerson.create().withRole("APPLICANT").withPartyId("party-1"),
				FaPerson.create().withRole("CO_APPLICANT").withPartyId("party-2")))));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, "party-1")).thenReturn(Optional.of("19800101T001"));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, "party-2")).thenReturn(Optional.of("19850505T002"));

		final var context = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(context.errandNumber()).isEqualTo(ERRAND_NUMBER);
		assertThat(context.applicantPersonId()).isEqualTo("19800101T001");
		assertThat(context.coApplicantPersonId()).isEqualTo("19850505T002");
	}

	@Test
	void missingCoApplicantAndUnresolvableApplicantYieldNulls() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID).withErrandNumber(ERRAND_NUMBER));
		when(stakeholderServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create()
			.withPersons(List.of(FaPerson.create().withRole("APPLICANT").withPartyId("party-1")))));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, "party-1")).thenReturn(Optional.empty());

		final var context = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(context.errandNumber()).isEqualTo(ERRAND_NUMBER);
		assertThat(context.applicantPersonId()).isNull();
		assertThat(context.coApplicantPersonId()).isNull();
	}

	@Test
	void stakeholderPartyIdWinsOverEmptyApplicationPersons() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID).withErrandNumber(ERRAND_NUMBER));
		when(stakeholderServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of(
			Stakeholder.create().withRole("APPLICANT").withExternalId("party-1").withExternalIdType("PRIVATE")));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create()
			.withPersons(List.of())));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, "party-1")).thenReturn(Optional.of("19800101T001"));

		final var context = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(context.errandNumber()).isEqualTo(ERRAND_NUMBER);
		assertThat(context.applicantPersonId()).isEqualTo("19800101T001");
		assertThat(context.coApplicantPersonId()).isNull();
	}

	@Test
	void missingApplicationPayloadYieldsErrandNumberOnly() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(Errand.create().withId(ERRAND_ID).withErrandNumber(ERRAND_NUMBER));
		when(stakeholderServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(List.of());
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());

		final var context = service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(context.errandNumber()).isEqualTo(ERRAND_NUMBER);
		assertThat(context.applicantPersonId()).isNull();
		assertThat(context.coApplicantPersonId()).isNull();
		verifyNoInteractions(citizenServiceMock);
	}

	@Test
	void unknownErrandIsNotFound() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenThrow(Problem.valueOf(NOT_FOUND, "No errand"));

		assertThatThrownBy(() -> service.get(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
		verifyNoInteractions(repositoryMock, citizenServiceMock);
	}
}
