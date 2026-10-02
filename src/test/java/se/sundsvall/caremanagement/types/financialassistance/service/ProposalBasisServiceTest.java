package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseHistoryService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseService;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class ProposalBasisServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "my-namespace";
	private static final String ERRAND_ID = "errand-1";
	private static final String APPLICANT = "pnr-a";

	@Mock
	private ErrandService errandServiceMock;

	@Mock
	private DraftService draftServiceMock;

	@Mock
	private HouseholdPartyService householdPartyServiceMock;

	@Mock
	private LifecareCaseService lifecareCaseServiceMock;

	@Mock
	private LifecareCaseHistoryService lifecareCaseHistoryServiceMock;

	@Mock
	private FinancialAssistanceRepository financialAssistanceRepositoryMock;

	@InjectMocks
	private ProposalBasisService service;

	private static HouseholdPartyService.Household household(final Optional<String> applicant) {
		return new HouseholdPartyService.Household(applicant, false, Optional.empty(), Optional.empty());
	}

	private static CalculationView calculation(final Integer id, final BigDecimal normSum, final BigDecimal balance) {
		return new CalculationView(id, "Riksnorm", "2026-06-01", "2026-06-30", null, null, null, normSum, null, null, balance, null, true, List.of(), List.of(), List.of(), List.of());
	}

	private static CalculationDraft estimableDraft() {
		return CalculationDraft.create().withApplicationMonth("2026-06").withIncomeSum(new BigDecimal("3000")).withExpenseSum(new BigDecimal("800")).withSpecialExpenseSum(new BigDecimal("250"));
	}

	private void linkCalculation(final Integer lifecareCalculationId) {
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID)).thenReturn(Optional.of(FinancialAssistanceEntity.create().withLifecareCalculationId(lifecareCalculationId)));
	}

	@Test
	void basisWithNormEstimatesTheAmount() {
		// FamilyCare carries the norm negated; the estimate adds it as the cost it is, or every underskott reads as avslag.
		final var draft = CalculationDraft.create().withApplicationMonth("2026-06").withIncomeSum(new BigDecimal("3000")).withExpenseSum(new BigDecimal("800")).withSpecialExpenseSum(new BigDecimal("250"));
		when(draftServiceMock.get(ERRAND_ID)).thenReturn(draft);
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(household(Optional.of(APPLICANT)));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, APPLICANT, YearMonth.parse("2026-06"))).thenReturn(new PreviousHousehold(Set.of(), true, 1, new BigDecimal("-6200"), null, "Riksnorm"));

		final var basis = service.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		assertThat(basis.draft()).isSameAs(draft);
		assertThat(basis.applicationMonth()).contains(YearMonth.parse("2026-06"));
		assertThat(basis.normSum()).contains(new BigDecimal("6200"));
		assertThat(basis.estimatedAmount()).contains(new BigDecimal("4250"));
		assertThat(basis.amountBasis()).contains(ProposalBasisService.AMOUNT_BASIS_ESTIMATE);
		assertThat(basis.savedCalculation()).isEmpty();
		assertThat(basis.lifecareServiceId()).isEmpty();
		verify(lifecareCaseHistoryServiceMock, never()).listCalculations(MUNICIPALITY_ID, APPLICANT, LocalDate.parse("2026-04-01"), LocalDate.parse("2026-08-30"));
	}

	@Test
	void savedLifecareCalculationDecidesTheAmount() {
		// Lifecare's result differs from the draft estimate (4250) — jobbstimulans, say. Lifecare's wins. The listing is
		// filtered on the calculation date, so the window reaches two months either side of the period.
		when(draftServiceMock.get(ERRAND_ID)).thenReturn(estimableDraft());
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(household(Optional.of(APPLICANT)));
		when(financialAssistanceRepositoryMock.findByErrandId(ERRAND_ID))
			.thenReturn(Optional.of(FinancialAssistanceEntity.create().withLifecareCalculationId(42).withLifecareServiceId(25)));
		when(lifecareCaseHistoryServiceMock.listCalculations(MUNICIPALITY_ID, APPLICANT, LocalDate.parse("2026-04-01"), LocalDate.parse("2026-08-30")))
			.thenReturn(List.of(calculation(41, new BigDecimal("-6000"), new BigDecimal("-100")), calculation(42, new BigDecimal("-6300"), new BigDecimal("-2820"))));

		final var basis = service.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(basis.estimatedAmount()).contains(new BigDecimal("2820"));
		assertThat(basis.normSum()).contains(new BigDecimal("6300"));
		assertThat(basis.lifecareServiceId()).contains(25);
		assertThat(basis.amountBasis()).contains(ProposalBasisService.AMOUNT_BASIS_LIFECARE_CALCULATION);
		assertThat(basis.savedCalculation()).hasValueSatisfying(calculation -> assertThat(calculation.id()).isEqualTo(42));
		verifyNoInteractions(lifecareCaseServiceMock);
	}

	@Test
	void savedLifecareCalculationWithSurplusGivesANonPositiveAmount() {
		when(draftServiceMock.get(ERRAND_ID)).thenReturn(estimableDraft().withCalculationFromDate(LocalDate.parse("2026-06-15")).withCalculationToDate(LocalDate.parse("2026-06-30")));
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(household(Optional.of(APPLICANT)));
		linkCalculation(42);
		when(lifecareCaseHistoryServiceMock.listCalculations(MUNICIPALITY_ID, APPLICANT, LocalDate.parse("2026-04-15"), LocalDate.parse("2026-08-30")))
			.thenReturn(List.of(calculation(42, new BigDecimal("6300"), new BigDecimal("500"))));

		final var basis = service.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(basis.estimatedAmount()).contains(new BigDecimal("-500"));
		assertThat(basis.amountBasis()).contains(ProposalBasisService.AMOUNT_BASIS_LIFECARE_CALCULATION);
	}

	@Test
	void savedLifecareCalculationNotFoundFallsBackToTheEstimate() {
		when(draftServiceMock.get(ERRAND_ID)).thenReturn(estimableDraft());
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(household(Optional.of(APPLICANT)));
		linkCalculation(42);
		when(lifecareCaseHistoryServiceMock.listCalculations(MUNICIPALITY_ID, APPLICANT, LocalDate.parse("2026-04-01"), LocalDate.parse("2026-08-30")))
			.thenReturn(List.of(calculation(41, new BigDecimal("6000"), new BigDecimal("-100")), calculation(42, new BigDecimal("6300"), null)));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, APPLICANT, YearMonth.parse("2026-06"))).thenReturn(new PreviousHousehold(Set.of(), true, 1, new BigDecimal("6200"), null, "Riksnorm"));

		final var basis = service.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(basis.estimatedAmount()).contains(new BigDecimal("4250"));
		assertThat(basis.amountBasis()).contains(ProposalBasisService.AMOUNT_BASIS_ESTIMATE);
	}

	@Test
	void aFailedSavedCalculationReadFallsBackToTheEstimate() {
		when(draftServiceMock.get(ERRAND_ID)).thenReturn(estimableDraft());
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(household(Optional.of(APPLICANT)));
		linkCalculation(42);
		when(lifecareCaseHistoryServiceMock.listCalculations(MUNICIPALITY_ID, APPLICANT, LocalDate.parse("2026-04-01"), LocalDate.parse("2026-08-30")))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "down"));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, APPLICANT, YearMonth.parse("2026-06"))).thenReturn(new PreviousHousehold(Set.of(), true, 1, new BigDecimal("6200"), null, "Riksnorm"));

		final var basis = service.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(basis.estimatedAmount()).contains(new BigDecimal("4250"));
		assertThat(basis.amountBasis()).contains(ProposalBasisService.AMOUNT_BASIS_ESTIMATE);
	}

	@Test
	void basisWithoutPreviousNormLeavesTheAmountUnknown() {
		when(draftServiceMock.get(ERRAND_ID)).thenReturn(CalculationDraft.create().withApplicationMonth("2026-06"));
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(household(Optional.of(APPLICANT)));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, APPLICANT, YearMonth.parse("2026-06"))).thenReturn(PreviousHousehold.empty());

		final var basis = service.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(basis.normSum()).isEmpty();
		assertThat(basis.estimatedAmount()).isEmpty();
		assertThat(basis.amountBasis()).isEmpty();
	}

	@Test
	void basisToleratesAFailedLifecareRead() {
		when(draftServiceMock.get(ERRAND_ID)).thenReturn(CalculationDraft.create().withApplicationMonth("2026-06"));
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(household(Optional.of(APPLICANT)));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, APPLICANT, YearMonth.parse("2026-06"))).thenThrow(Problem.valueOf(BAD_GATEWAY, "down"));

		assertThat(service.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID).estimatedAmount()).isEmpty();
	}

	@Test
	void basisWithoutApplicantOrMonthSkipsLifecare() {
		when(draftServiceMock.get(ERRAND_ID)).thenReturn(CalculationDraft.create());
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(household(Optional.empty()));

		final var basis = service.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(basis.applicationMonth()).isEmpty();
		assertThat(basis.normSum()).isEmpty();
		verifyNoInteractions(lifecareCaseServiceMock, lifecareCaseHistoryServiceMock);
	}
}
