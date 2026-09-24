package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationCalculationIncomeTypeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationNormDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationProposalDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationServiceDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareIntegration;
import se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole;
import se.sundsvall.caremanagement.lifecare.service.model.ClassifiedIncome;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;
import tools.jackson.databind.ObjectMapper;

import static java.time.Month.JUNE;
import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	private static final String APPLICANT = "199001011234";
	private static final YearMonth MONTH = YearMonth.of(2026, JUNE);

	@Mock
	private LifecareFamilyCareIntegration lifecareFamilyCareIntegrationMock;

	@Mock
	private LifecareCaseService lifecareCaseServiceMock;

	@Mock
	private ObjectMapper objectMapperMock;

	@InjectMocks
	private CalculationService service;

	private static ClassifiedIncome bostadsbidrag() {
		return new ClassifiedIncome(
			new SsbtekIncome("Bostadsbidrag", null, "Månad", new BigDecimal("1850"), LocalDate.of(2026, MAY, 15), ApplicantRole.APPLICANT),
			"TA_MED_KVITTNING", "Bostadsbidrag", false, "Ta med kvittning");
	}

	private static PersonBasedCalculationProposalDTO proposal() {
		return new PersonBasedCalculationProposalDTO()
			.addServicesItem(new PersonBasedCalculationServiceDTO().id(5))
			.addCalculationIncomeTypesItem(new PersonBasedCalculationCalculationIncomeTypeDTO().id(20).name("Bostadsbidrag"));
	}

	@Test
	void incomeLinesResolvesPerRecipientRows() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH)).thenReturn(List.of());

		final var lines = service.incomeLines(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]");

		assertThat(lines).singleElement().satisfies(line -> {
			assertThat(line.typeId()).isEqualTo(20);
			assertThat(line.typeName()).isEqualTo("Bostadsbidrag");
			assertThat(line.recipient()).isEqualTo("APPLICANT");
			assertThat(line.amount()).isEqualByComparingTo("1850");
		});
	}

	@Test
	void lateTransferredComparisonIncomesReportsWhatTheTransferPickedUp() {
		final var comparisonPeriod = new ClassifiedIncome(
			new SsbtekIncome("Underhållsstöd", null, "Månad", new BigDecimal("1673"), LocalDate.of(2026, MAY, 15), ApplicantRole.APPLICANT),
			"TA_MED", "Underhållsstöd", false, "Ta med", true);
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			comparisonPeriod, bostadsbidrag()
		});
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH)).thenReturn(List.of());

		final var late = service.lateTransferredComparisonIncomes(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]");

		// Only the comparison-period income the previous calculation lacked — the control-period one is not a late arrival.
		assertThat(late).singleElement().satisfies(income -> assertThat(income.income().benefit()).isEqualTo("Underhållsstöd"));
	}

	@Test
	void lateTransferredComparisonIncomesIsEmptyWhenThePreviousMonthAlreadyTookThem() {
		final var comparisonPeriod = new ClassifiedIncome(
			new SsbtekIncome("Underhållsstöd", null, "Månad", new BigDecimal("1673"), LocalDate.of(2026, MAY, 15), ApplicantRole.APPLICANT),
			"TA_MED", "Underhållsstöd", false, "Ta med", true);
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			comparisonPeriod
		});
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH)).thenReturn(List.of("Underhållsstöd"));

		// Nothing moved, so there is nothing to warn about — the money was already counted last month.
		assertThat(service.lateTransferredComparisonIncomes(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]")).isEmpty();
	}

	@Test
	void incomeLinesTransfersTheComparisonPeriodUnfilteredWhenThePreviousMonthCannotBeRead() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH))
			.thenThrow(new IllegalStateException("Lifecare unavailable"));

		// an income counted twice surfaces as a duplicate warning; one silently withheld surfaces as nothing
		assertThat(service.incomeLines(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]")).hasSize(1);
	}

	@Test
	void completenessTreatedAsCompleteWhenThePreviousLookupFails() {
		// Best-effort: a Lifecare outage must not wedge the financial assistance process on an incomplete verdict.
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH)).thenThrow(new RuntimeException("FamilyCare down"));

		final var completeness = service.completeness(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]");

		assertThat(completeness.informationComplete()).isTrue();
		assertThat(completeness.missingIncomeTypes()).isEmpty();
	}

	@Test
	void completenessReportsMissingPreviousTypes() {
		when(objectMapperMock.readValue("[json]", ClassifiedIncome[].class)).thenReturn(new ClassifiedIncome[] {
			bostadsbidrag()
		});
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal());
		when(lifecareCaseServiceMock.previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT, MONTH)).thenReturn(List.of("Bostadsbidrag", "Dagersättning"));

		final var completeness = service.completeness(MUNICIPALITY_ID, APPLICANT, MONTH, "[json]");

		assertThat(completeness.informationComplete()).isFalse();
		assertThat(completeness.missingIncomeTypes()).containsExactly("Dagersättning");
	}

	@Test
	void selectNormIdPicksTheNormCoveringTheMonth() {
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(99).fromDate("2026-01-01").toDate("2026-12-31")));

		assertThat(service.selectNormId(MUNICIPALITY_ID, APPLICANT, MONTH, List.of(), List.of()))
			.isEqualTo(new CalculationService.NormChoice(99, false));
	}

	@Test
	void selectNormIdPrefersThePreviousNormAndSaysSo() {
		when(lifecareFamilyCareIntegrationMock.getCalculationProposal(MUNICIPALITY_ID, APPLICANT)).thenReturn(proposal()
			.addNormsItem(new PersonBasedCalculationNormDTO().id(4).name("Matnorm 2026").fromDate("2026-01-01").toDate("2026-12-31"))
			.addNormsItem(new PersonBasedCalculationNormDTO().id(1).name("Riksnorm 2026").fromDate("2026-01-01").toDate("2026-12-31")));

		assertThat(service.selectNormId(MUNICIPALITY_ID, APPLICANT, MONTH, List.of("Matnorm"), List.of("Riksnorm")))
			.isEqualTo(new CalculationService.NormChoice(4, true));
		// a previous norm the month does not offer: the application's is chosen, and the choice says it was not preferred
		assertThat(service.selectNormId(MUNICIPALITY_ID, APPLICANT, MONTH, List.of("Specnorm"), List.of("Riksnorm")))
			.isEqualTo(new CalculationService.NormChoice(1, false));
		verify(lifecareFamilyCareIntegrationMock, times(2)).getCalculationProposal(MUNICIPALITY_ID, APPLICANT);
	}
}
