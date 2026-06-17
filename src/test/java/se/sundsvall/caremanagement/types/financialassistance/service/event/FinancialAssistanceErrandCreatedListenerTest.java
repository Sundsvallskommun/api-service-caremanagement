package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.operaton.service.ProcessService;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_NEW;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUG_RENEWAL;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedListener.PROCESS_DEFINITION_NAME;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedListener.VAR_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedListener.VAR_APPLICATION_MONTH;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedListener.VAR_CO_APPLICANT;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedListener.VAR_CO_APPLICANT_PERSONAL_NUMBER;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedListener.VAR_FROM_DATE;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedListener.VAR_MUNICIPALITY_ID;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedListener.VAR_NAMESPACE;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedListener.VAR_PERSONAL_NUMBER;
import static se.sundsvall.caremanagement.types.financialassistance.service.event.FinancialAssistanceErrandCreatedListener.VAR_TO_DATE;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceErrandCreatedListenerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "errand-1";
	private static final String PROCESS_INSTANCE_ID = "proc-1";
	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String CO_APPLICANT_PARTY_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
	private static final String APPLICANT_PERSONAL_NUMBER = "199001011234";
	private static final String CO_APPLICANT_PERSONAL_NUMBER = "198202022345";

	@Mock
	private FinancialAssistanceRepository repositoryMock;

	@Mock
	private ProcessService processServiceMock;

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private CitizenService citizenServiceMock;

	@InjectMocks
	private FinancialAssistanceErrandCreatedListener listener;

	private static ErrandCreated event(final String typeSlug) {
		return new ErrandCreated(ERRAND_ID, typeSlug, MUNICIPALITY_ID, NAMESPACE, "reporter", "assignee",
			OffsetDateTime.parse("2026-06-05T12:00:00Z"));
	}

	@Test
	void startsProcessWithHouseholdVariablesAndLinksInstance() {
		final var entity = FinancialAssistanceEntity.create()
			.withErrandId(ERRAND_ID)
			.withPeriodYear(2026)
			.withPeriodMonth(6)
			.withPersons(List.of(
				FaPerson.create().withRole("APPLICANT").withPartyId(APPLICANT_PARTY_ID),
				FaPerson.create().withRole("CO_APPLICANT").withPartyId(CO_APPLICANT_PARTY_ID)));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(APPLICANT_PERSONAL_NUMBER));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, CO_APPLICANT_PARTY_ID)).thenReturn(Optional.of(CO_APPLICANT_PERSONAL_NUMBER));
		when(processServiceMock.startProcess(eq(MUNICIPALITY_ID), eq(PROCESS_DEFINITION_NAME), eq(ERRAND_ID), any()))
			.thenReturn(Optional.of(PROCESS_INSTANCE_ID));

		listener.on(event(SLUG_RENEWAL));

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(processServiceMock).startProcess(eq(MUNICIPALITY_ID), eq(PROCESS_DEFINITION_NAME), eq(ERRAND_ID), varsCaptor.capture());
		assertThat(varsCaptor.getValue())
			.containsEntry(VAR_MUNICIPALITY_ID, MUNICIPALITY_ID)
			.containsEntry(VAR_NAMESPACE, NAMESPACE)
			.containsEntry(VAR_APPLICANT, APPLICANT_PARTY_ID)
			.containsEntry(VAR_CO_APPLICANT, CO_APPLICANT_PARTY_ID)
			.containsEntry(VAR_APPLICATION_MONTH, "2026-06")
			.containsEntry(VAR_PERSONAL_NUMBER, APPLICANT_PERSONAL_NUMBER)
			.containsEntry(VAR_CO_APPLICANT_PERSONAL_NUMBER, CO_APPLICANT_PERSONAL_NUMBER)
			.containsEntry(VAR_FROM_DATE, "2026-04-01")
			.containsEntry(VAR_TO_DATE, "2026-06-30");

		verify(errandServiceMock).linkProcessInstance(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PROCESS_INSTANCE_ID);
	}

	@Test
	void omitsCoApplicantAndMonthWhenAbsent() {
		final var entity = FinancialAssistanceEntity.create()
			.withErrandId(ERRAND_ID)
			.withPersons(List.of(FaPerson.create().withRole("APPLICANT").withPartyId(APPLICANT_PARTY_ID)));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(APPLICANT_PERSONAL_NUMBER));
		when(processServiceMock.startProcess(eq(MUNICIPALITY_ID), eq(PROCESS_DEFINITION_NAME), eq(ERRAND_ID), any()))
			.thenReturn(Optional.of(PROCESS_INSTANCE_ID));

		listener.on(event(SLUG_RENEWAL));

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(processServiceMock).startProcess(eq(MUNICIPALITY_ID), eq(PROCESS_DEFINITION_NAME), eq(ERRAND_ID), varsCaptor.capture());
		assertThat(varsCaptor.getValue())
			.containsEntry(VAR_APPLICANT, APPLICANT_PARTY_ID)
			.containsEntry(VAR_PERSONAL_NUMBER, APPLICANT_PERSONAL_NUMBER)
			.containsEntry(VAR_CO_APPLICANT_PERSONAL_NUMBER, "")
			.doesNotContainKey(VAR_CO_APPLICANT)
			.doesNotContainKey(VAR_APPLICATION_MONTH)
			.doesNotContainKey(VAR_FROM_DATE)
			.doesNotContainKey(VAR_TO_DATE);

		verify(errandServiceMock).linkProcessInstance(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, PROCESS_INSTANCE_ID);
	}

	@Test
	void ignoresNonRenewalSlug() {
		listener.on(event(SLUG_NEW));

		verifyNoInteractions(repositoryMock, processServiceMock, errandServiceMock);
	}

	@Test
	void noopWhenEntityMissing() {
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());

		listener.on(event(SLUG_RENEWAL));

		verify(repositoryMock).findByErrandId(ERRAND_ID);
		verifyNoMoreInteractions(repositoryMock);
		verifyNoInteractions(processServiceMock, errandServiceMock);
	}

	@Test
	void swallowsProcessStartFailureAndDoesNotLink() {
		final var entity = FinancialAssistanceEntity.create()
			.withErrandId(ERRAND_ID)
			.withPersons(List.of(FaPerson.create().withRole("APPLICANT").withPartyId(APPLICANT_PARTY_ID)));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(APPLICANT_PERSONAL_NUMBER));
		when(processServiceMock.startProcess(eq(MUNICIPALITY_ID), eq(PROCESS_DEFINITION_NAME), eq(ERRAND_ID), any()))
			.thenThrow(Problem.valueOf(BAD_REQUEST, "No Operaton process definition found"));

		assertThatCode(() -> listener.on(event(SLUG_RENEWAL))).doesNotThrowAnyException();

		verify(errandServiceMock, never()).linkProcessInstance(any(), any(), any(), any());
	}

	@Test
	void doesNotLinkWhenNoProcessStarted() {
		final var entity = FinancialAssistanceEntity.create()
			.withErrandId(ERRAND_ID)
			.withPersons(List.of(FaPerson.create().withRole("APPLICANT").withPartyId(APPLICANT_PARTY_ID)));
		when(repositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(entity));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(APPLICANT_PERSONAL_NUMBER));
		when(processServiceMock.startProcess(eq(MUNICIPALITY_ID), eq(PROCESS_DEFINITION_NAME), eq(ERRAND_ID), any()))
			.thenReturn(Optional.empty());

		listener.on(event(SLUG_RENEWAL));

		verify(errandServiceMock, never()).linkProcessInstance(any(), any(), any(), any());
	}
}
