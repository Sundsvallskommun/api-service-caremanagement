package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.financialaid.integration.FinancialAidIntegration;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.service.StakeholderService;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.time.Month.APRIL;
import static java.time.Month.JANUARY;
import static java.time.Month.JUNE;
import static java.time.Month.MARCH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class FinancialAssistanceSsbtekServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String APPLICANT_PARTY_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
	private static final String PERSONAL_NUMBER = "199001011234";
	private static final String NAMESPACE = "FINANCIAL_ASSISTANCE";
	private static final String ERRAND_ID = "8d6a1d52-6f8c-4a64-9a55-0b8e9f7b2c11";
	private static final String CO_APPLICANT_PARTY_ID = "0c9f2a8e-3d41-4b7a-9c2e-5f6a7b8c9d01";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private StakeholderService stakeholderServiceMock;

	@Mock
	private FinancialAssistanceRepository financialAssistanceRepositoryMock;

	@Mock
	private CitizenService citizenServiceMock;

	@Mock
	private FinancialAidIntegration financialAidIntegrationMock;

	@InjectMocks
	private FinancialAssistanceSsbtekService service;

	/** The errand's applicant is resolved from its stakeholders, the way the beredning resolves it. */
	@BeforeEach
	void householdOfOneApplicant() {
		lenient().when(stakeholderServiceMock.readAll(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID))
			.thenReturn(List.of(Stakeholder.create().withRole("APPLICANT").withExternalId(APPLICANT_PARTY_ID)));
	}

	@Test
	void getBasisReadsTheCoApplicantFromTheErrandsPersons() {
		// no co-applicant stakeholder; the application's person row carries the co-applicant
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create()
			.withPersons(List.of(FaPerson.create().withRole("CO_APPLICANT").withPartyId(CO_APPLICANT_PARTY_ID)))));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, CO_APPLICANT_PARTY_ID)).thenReturn(Optional.of("199202021234"));
		when(financialAidIntegrationMock.getFinancialAidBasis(MUNICIPALITY_ID, "199202021234", "2026-01-01", "2026-03-31")).thenReturn(Map.of());

		final var result = service.getBasis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "CO_APPLICANT", LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, MARCH, 31));

		assertThat(result.getAgencies()).isEmpty();
		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
	}

	@Test
	void getBasisIs404WithoutAMemberInTheRole() {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getBasis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "CO_APPLICANT", null, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessageContaining("has no household member with role CO_APPLICANT");
		verifyNoInteractions(citizenServiceMock, financialAidIntegrationMock);
	}

	@Test
	void getBasisIs404ForAnErrandOutsideTheNamespace() {
		when(errandServiceMock.readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenThrow(Problem.valueOf(NOT_FOUND, "No errand"));

		assertThatThrownBy(() -> service.getBasis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "APPLICANT", null, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
		verifyNoInteractions(stakeholderServiceMock, citizenServiceMock, financialAidIntegrationMock);
	}

	@Test
	void getBasisResolvesPartyAndForwardsAnswerVerbatim() {
		final var agencies = Map.<String, Map<String, Object>>of("fk", Map.of("formansinformation", Map.of("utbetalningsuppgift", "…")));
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSONAL_NUMBER));
		when(financialAidIntegrationMock.getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, "2026-01-01", "2026-03-31")).thenReturn(agencies);

		final var result = service.getBasis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "APPLICANT", LocalDate.of(2026, JANUARY, 1), LocalDate.of(2026, MARCH, 31));

		assertThat(result.getFrom()).isEqualTo(LocalDate.of(2026, JANUARY, 1));
		assertThat(result.getTo()).isEqualTo(LocalDate.of(2026, MARCH, 31));
		assertThat(result.getAgencies()).isSameAs(agencies);
		verify(financialAidIntegrationMock).getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, "2026-01-01", "2026-03-31");
	}

	@Test
	void getBasisDefaultsToTheThreeRulePeriods() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSONAL_NUMBER));
		when(financialAidIntegrationMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(Map.of());

		final var result = service.getBasis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "APPLICANT", null, null);

		final var expectedFrom = YearMonth.now().minusMonths(2).atDay(1);
		final var expectedTo = YearMonth.now().atEndOfMonth();
		assertThat(result.getFrom()).isEqualTo(expectedFrom);
		assertThat(result.getTo()).isEqualTo(expectedTo);
		verify(financialAidIntegrationMock).getFinancialAidBasis(MUNICIPALITY_ID, PERSONAL_NUMBER, expectedFrom.toString(), expectedTo.toString());
	}

	@Test
	void getBasisDerivesEndFromGivenStart() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSONAL_NUMBER));
		when(financialAidIntegrationMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(Map.of());

		final var result = service.getBasis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "APPLICANT", LocalDate.of(2026, APRIL, 10), null);

		assertThat(result.getFrom()).isEqualTo(LocalDate.of(2026, APRIL, 10));
		assertThat(result.getTo()).isEqualTo(LocalDate.of(2026, JUNE, 30));
	}

	@Test
	void getBasisDerivesStartFromGivenEnd() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSONAL_NUMBER));
		when(financialAidIntegrationMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(Map.of());

		final var result = service.getBasis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "APPLICANT", null, LocalDate.of(2026, JUNE, 15));

		assertThat(result.getFrom()).isEqualTo(LocalDate.of(2026, APRIL, 1));
		assertThat(result.getTo()).isEqualTo(LocalDate.of(2026, JUNE, 15));
	}

	@Test
	void getBasisRendersAMissingAnswerAsAnEmptyMap() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSONAL_NUMBER));
		when(financialAidIntegrationMock.getFinancialAidBasis(any(), any(), any(), any())).thenReturn(null);

		final var result = service.getBasis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "APPLICANT", null, null);

		assertThat(result.getAgencies()).isNotNull().isEmpty();
	}

	@Test
	void getBasisThrowsNotFoundForUnknownParty() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getBasis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "APPLICANT", null, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verifyNoInteractions(financialAidIntegrationMock);
	}

	@Test
	void getBasisRejectsAnInvertedPeriodBeforeCallingSsbtek() {
		when(citizenServiceMock.getPersonalNumber(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(Optional.of(PERSONAL_NUMBER));

		assertThatThrownBy(() -> service.getBasis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID, "APPLICANT", LocalDate.of(2026, JUNE, 1), LocalDate.of(2026, JANUARY, 1)))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);

		verify(financialAidIntegrationMock, never()).getFinancialAidBasis(any(), any(), any(), any());
	}
}
