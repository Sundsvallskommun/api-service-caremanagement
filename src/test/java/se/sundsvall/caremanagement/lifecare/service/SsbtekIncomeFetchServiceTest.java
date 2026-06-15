package se.sundsvall.caremanagement.lifecare.service;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.financialaid.integration.FinancialAidIntegration;
import se.sundsvall.caremanagement.lifecare.service.model.PreparedSsbtekIncomes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SsbtekIncomeFetchServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String APPLICANT = "200001012384";
	private static final String CO_APPLICANT = "200102034852";
	private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
	private static final String FROM_DATE = "2026-03-01"; // jämförelseperiod (March) start
	private static final String TO_DATE = "2026-04-30";    // kontrollperiod (April) end

	@Mock
	private FinancialAidIntegration financialAidIntegrationMock;

	@Mock
	private SsbtekIncomeService ssbtekIncomeServiceMock;

	@InjectMocks
	private SsbtekIncomeFetchService service;

	private static final PreparedSsbtekIncomes PREPARED = new PreparedSsbtekIncomes(List.of(), List.of(), List.of());

	@Test
	void fetchesApplicantOnlyOverThePeriodRangeAndDelegates() {
		final var applicantBasis = Map.<String, Map<String, Object>>of("so", Map.of());
		when(financialAidIntegrationMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT, FROM_DATE, TO_DATE)).thenReturn(applicantBasis);
		when(ssbtekIncomeServiceMock.prepareCalculationIncomes(APPLICANT, applicantBasis, null, MAY_2026)).thenReturn(PREPARED);

		final var result = service.fetchAndPrepare(MUNICIPALITY_ID, APPLICANT, null, MAY_2026);

		assertThat(result).isSameAs(PREPARED);
		verify(financialAidIntegrationMock).getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT, FROM_DATE, TO_DATE);
		verify(ssbtekIncomeServiceMock).prepareCalculationIncomes(APPLICANT, applicantBasis, null, MAY_2026);
		verifyNoMoreInteractions(financialAidIntegrationMock, ssbtekIncomeServiceMock);
	}

	@Test
	void fetchesBothPersonsWhenCoApplicantPresent() {
		final var applicantBasis = Map.<String, Map<String, Object>>of("so", Map.of("a", Map.of()));
		final var coApplicantBasis = Map.<String, Map<String, Object>>of("so", Map.of("b", Map.of()));
		when(financialAidIntegrationMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT, FROM_DATE, TO_DATE)).thenReturn(applicantBasis);
		when(financialAidIntegrationMock.getFinancialAidBasis(MUNICIPALITY_ID, CO_APPLICANT, FROM_DATE, TO_DATE)).thenReturn(coApplicantBasis);
		when(ssbtekIncomeServiceMock.prepareCalculationIncomes(APPLICANT, applicantBasis, coApplicantBasis, MAY_2026)).thenReturn(PREPARED);

		final var result = service.fetchAndPrepare(MUNICIPALITY_ID, APPLICANT, CO_APPLICANT, MAY_2026);

		assertThat(result).isSameAs(PREPARED);
		verify(financialAidIntegrationMock).getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT, FROM_DATE, TO_DATE);
		verify(financialAidIntegrationMock).getFinancialAidBasis(MUNICIPALITY_ID, CO_APPLICANT, FROM_DATE, TO_DATE);
		verify(ssbtekIncomeServiceMock).prepareCalculationIncomes(APPLICANT, applicantBasis, coApplicantBasis, MAY_2026);
		verifyNoMoreInteractions(financialAidIntegrationMock, ssbtekIncomeServiceMock);
	}

	@Test
	void blankCoApplicantIsTreatedAsAbsent() {
		final var applicantBasis = Map.<String, Map<String, Object>>of("so", Map.of());
		when(financialAidIntegrationMock.getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT, FROM_DATE, TO_DATE)).thenReturn(applicantBasis);
		when(ssbtekIncomeServiceMock.prepareCalculationIncomes(APPLICANT, applicantBasis, null, MAY_2026)).thenReturn(PREPARED);

		service.fetchAndPrepare(MUNICIPALITY_ID, APPLICANT, "  ", MAY_2026);

		verify(financialAidIntegrationMock).getFinancialAidBasis(MUNICIPALITY_ID, APPLICANT, FROM_DATE, TO_DATE);
		verify(ssbtekIncomeServiceMock).prepareCalculationIncomes(APPLICANT, applicantBasis, null, MAY_2026);
		verifyNoMoreInteractions(financialAidIntegrationMock, ssbtekIncomeServiceMock);
	}
}
