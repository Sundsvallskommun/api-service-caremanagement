package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.core.service.ErrandService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseService;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
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

	@InjectMocks
	private ProposalBasisService service;

	private static HouseholdPartyService.Household household(final Optional<String> applicant) {
		return new HouseholdPartyService.Household(applicant, false, Optional.empty(), Optional.empty());
	}

	@Test
	void basisWithNormEstimatesTheAmount() {
		final var draft = CalculationDraft.create().withApplicationMonth("2026-06").withIncomeSum(new BigDecimal("3000")).withExpenseSum(new BigDecimal("800")).withSpecialExpenseSum(new BigDecimal("250"));
		when(draftServiceMock.get(ERRAND_ID)).thenReturn(draft);
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(household(Optional.of(APPLICANT)));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, APPLICANT, YearMonth.parse("2026-06"))).thenReturn(new PreviousHousehold(Set.of(), true, 1, new BigDecimal("6200"), null, "Riksnorm"));

		final var basis = service.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		verify(errandServiceMock).readErrand(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		assertThat(basis.draft()).isSameAs(draft);
		assertThat(basis.applicationMonth()).contains(YearMonth.parse("2026-06"));
		assertThat(basis.normSum()).contains(new BigDecimal("6200"));
		assertThat(basis.estimatedAmount()).contains(new BigDecimal("4250"));
	}

	@Test
	void basisWithoutPreviousNormLeavesTheAmountUnknown() {
		when(draftServiceMock.get(ERRAND_ID)).thenReturn(CalculationDraft.create().withApplicationMonth("2026-06"));
		when(householdPartyServiceMock.household(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(household(Optional.of(APPLICANT)));
		when(lifecareCaseServiceMock.previousHousehold(MUNICIPALITY_ID, APPLICANT, YearMonth.parse("2026-06"))).thenReturn(PreviousHousehold.empty());

		final var basis = service.basis(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);

		assertThat(basis.normSum()).isEmpty();
		assertThat(basis.estimatedAmount()).isEmpty();
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
		verifyNoInteractions(lifecareCaseServiceMock);
	}
}
