package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefc.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefc.CommonCalculationExpenseDTO;
import generated.se.sundsvall.lifecarefc.CommonCalculationIncomeDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedCalculationPersonDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedDecisionPersonDTO;
import generated.se.sundsvall.lifecarefc.PersonBasedPersonDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFcIntegration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecareEbCaseServiceTest {

	private static final String APPLICANT = "198001012389";
	private static final LocalDate REFERENCE = LocalDate.of(2026, 6, 15);

	@Mock
	private LifecareFcIntegration integrationMock;

	private LifecareEbCaseService service() {
		return new LifecareEbCaseService(integrationMock, 13);
	}

	private void noActualisations() {
		when(integrationMock.getActualisations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO());
	}

	private void noCalculations() {
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO());
	}

	@Test
	void footprintFromDecisionsWithMonthRangeAndCoApplicant() {
		final var decision = new PersonBasedDecisionDTO()
			.fromDate("2026-05-01")
			.toDate("2026-06-30")
			.addDecisionPersonDTOsItem(new PersonBasedDecisionPersonDTO().personId("198202022397").isCoApplicant(true));

		noActualisations();
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO().addResultItem(decision));
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().addResultItem(new PersonBasedCalculationDTO()));

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.hasFootprint()).isTrue();
		assertThat(summary.decisionMonths()).containsExactlyInAnyOrder(YearMonth.of(2026, 5), YearMonth.of(2026, 6));
		assertThat(summary.latestDecisionPeriod()).isEqualTo(YearMonth.of(2026, 6));
		assertThat(summary.hasCalculation()).isTrue();
		assertThat(summary.hasCoApplicant()).isTrue();
	}

	@Test
	void footprintFromActualisationOnly() {
		when(integrationMock.getActualisations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO().addResultItem(new PersonBasedAktualiseringDTO().status("OPEN")));
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO());
		noCalculations();

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.hasFootprint()).isTrue();
		assertThat(summary.decisionMonths()).isEmpty();
		assertThat(summary.latestDecisionPeriod()).isNull();
		assertThat(summary.hasCoApplicant()).isFalse();
	}

	@Test
	void footprintFromCalculationOnly() {
		noActualisations();
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO());
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().addResultItem(new PersonBasedCalculationDTO()));

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.hasFootprint()).isTrue();
		assertThat(summary.hasCalculation()).isTrue();
	}

	@Test
	void noFootprintYieldsEmptySummary() {
		noActualisations();
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO());
		noCalculations();

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.hasFootprint()).isFalse();
		assertThat(summary.decisionMonths()).isEmpty();
		assertThat(summary.latestDecisionPeriod()).isNull();
		assertThat(summary.hasCalculation()).isFalse();
		assertThat(summary.hasCoApplicant()).isFalse();
	}

	@Test
	void nullCompositesAreToleratedAsEmpty() {
		when(integrationMock.getActualisations(eq(APPLICANT), any(), any(), any(), any(), any())).thenReturn(null);
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any())).thenReturn(null);
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any())).thenReturn(null);

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.hasFootprint()).isFalse();
		assertThat(summary.decisionMonths()).isEmpty();
	}

	@Test
	void decisionMonthsFromSingleDateAndCoApplicantScalar() {
		noActualisations();
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO()
				.addResultItem(new PersonBasedDecisionDTO().toDate("2026-06-30").coApplicant("198202022397")));
		noCalculations();

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.decisionMonths()).containsExactly(YearMonth.of(2026, 6));
		assertThat(summary.hasCoApplicant()).isTrue();
	}

	@Test
	void latestDecisionDrivesPeriodAndConstellation() {
		final var older = new PersonBasedDecisionDTO().toDate("2026-03-31").coApplicant("197001010000");
		final var newer = new PersonBasedDecisionDTO().toDate("2026-05-31"); // no co-applicant
		noActualisations();
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO().result(List.of(older, newer)));
		noCalculations();

		final var summary = service().summarize(APPLICANT, REFERENCE);

		assertThat(summary.latestDecisionPeriod()).isEqualTo(YearMonth.of(2026, 5));
		assertThat(summary.hasCoApplicant()).isFalse(); // newest decision has none
		assertThat(summary.decisionMonths()).containsExactlyInAnyOrder(YearMonth.of(2026, 3), YearMonth.of(2026, 5));
	}

	@Test
	void latestRosterReadsLatestCalculationMembersAndFlaggedCoApplicant() {
		final var olderCalc = new PersonBasedCalculationDTO().toDate("2026-04-30")
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId("198001019999").name("Old"));
		final var newerCalc = new PersonBasedCalculationDTO().toDate("2026-06-30")
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(APPLICANT).name("Anna"))
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId("201801012380").name("Kid"));
		final var decision = new PersonBasedDecisionDTO().toDate("2026-06-30")
			.addDecisionPersonDTOsItem(new PersonBasedDecisionPersonDTO().personId("198202022397").isCoApplicant(true));

		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of(olderCalc, newerCalc)));
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO().addResultItem(decision));

		final var roster = service().latestRoster(APPLICANT, REFERENCE);

		assertThat(roster.applicant()).isEqualTo(APPLICANT);
		assertThat(roster.coApplicant()).isEqualTo("198202022397");
		assertThat(roster.members()).extracting(LifecareEbRoster.Member::personalNumber).containsExactly(APPLICANT, "201801012380");
		assertThat(roster.members()).extracting(LifecareEbRoster.Member::name).containsExactly("Anna", "Kid");
	}

	@Test
	void latestRosterFiltersBlankPersonIdsAndFallsBackToScalarCoApplicant() {
		final var calc = new PersonBasedCalculationDTO().toDate("2026-06-30")
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(APPLICANT).name("Anna"))
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId("  ").name("Blank"));

		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().addResultItem(calc));
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO()
				.addResultItem(new PersonBasedDecisionDTO().toDate("2026-06-30").coApplicant("198202022397")));

		final var roster = service().latestRoster(APPLICANT, REFERENCE);

		assertThat(roster.members()).extracting(LifecareEbRoster.Member::personalNumber).containsExactly(APPLICANT);
		assertThat(roster.coApplicant()).isEqualTo("198202022397");
	}

	@Test
	void latestRosterEmptyWhenNoCalculationsOrDecisions() {
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO());
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO());

		final var roster = service().latestRoster(APPLICANT, REFERENCE);

		assertThat(roster.applicant()).isEqualTo(APPLICANT);
		assertThat(roster.coApplicant()).isNull();
		assertThat(roster.members()).isEmpty();
	}

	@Test
	void latestRosterToleratesNullComposites() {
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any())).thenReturn(null);
		when(integrationMock.getDecisions(eq(APPLICANT), any(), any(), any(), any(), any())).thenReturn(null);

		final var roster = service().latestRoster(APPLICANT, REFERENCE);

		assertThat(roster.members()).isEmpty();
		assertThat(roster.coApplicant()).isNull();
	}

	@Test
	void previousCalculationIncomeTypesFromLatestCalcBeforeApplicationMonth() {
		final var older = new PersonBasedCalculationDTO().toDate("2026-03-31")
			.addCalculationIncomesDTOsItem(new CommonCalculationIncomeDTO().type("Aktivitetsstöd"));
		final var previous = new PersonBasedCalculationDTO().toDate("2026-05-31")
			.addCalculationIncomesDTOsItem(new CommonCalculationIncomeDTO().type("Bostadsbidrag"))
			.addCalculationIncomesDTOsItem(new CommonCalculationIncomeDTO().type("Dagersättning"))
			.addCalculationIncomesDTOsItem(new CommonCalculationIncomeDTO().type("Bostadsbidrag")); // duplicate collapses
		final var thisMonth = new PersonBasedCalculationDTO().toDate("2026-06-30")
			.addCalculationIncomesDTOsItem(new CommonCalculationIncomeDTO().type("Bostadsbidrag"));
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of(older, previous, thisMonth)));

		final var types = service().previousCalculationIncomeTypes(APPLICANT, YearMonth.of(2026, 6));

		assertThat(types).containsExactlyInAnyOrder("Bostadsbidrag", "Dagersättning");
	}

	@Test
	void previousCalculationIncomeTypesEmptyWhenNoPriorCalc() {
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any())).thenReturn(null);

		assertThat(service().previousCalculationIncomeTypes(APPLICANT, YearMonth.of(2026, 6))).isEmpty();
	}

	@Test
	void previousHouseholdFromLatestCalcBeforeApplicationMonthWithHousingCostAndNormSum() {
		final var older = new PersonBasedCalculationDTO().toDate("2026-03-31")
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId("198001019999"));
		final var previous = new PersonBasedCalculationDTO().toDate("2026-05-31")
			.normSum(12345.0)
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(APPLICANT))
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId("201801012380"))
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId("  ")) // blank filtered out
			.addCalculationExpensesDTOsItem(new CommonCalculationExpenseDTO().type("Hyra/Rent").approvedAmount(6000.0))
			.addCalculationExpensesDTOsItem(new CommonCalculationExpenseDTO().type("Housing").appliedAmount(1500.0)) // approved null -> applied
			.addCalculationExpensesDTOsItem(new CommonCalculationExpenseDTO().type("Electricity").approvedAmount(900.0)); // not housing
		final var current = new PersonBasedCalculationDTO().toDate("2026-06-30"); // not strictly before June -> excluded
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of(older, previous, current)));

		final var household = service().previousHousehold(APPLICANT, YearMonth.of(2026, 6));

		assertThat(household.personIds()).containsExactlyInAnyOrder(APPLICANT, "201801012380");
		assertThat(household.memberCount()).isEqualTo(2);
		assertThat(household.normSum()).isEqualTo(12345.0);
		assertThat(household.housingCost()).isEqualTo(7500.0); // 6000 (approved) + 1500 (applied fallback)
	}

	@Test
	void previousHouseholdEmptyWhenNoPriorCalculation() {
		when(integrationMock.getCalculations(eq(APPLICANT), any(), any(), any(), any(), any())).thenReturn(null);

		final var household = service().previousHousehold(APPLICANT, YearMonth.of(2026, 6));

		assertThat(household.personIds()).isEmpty();
		assertThat(household.memberCount()).isZero();
		assertThat(household.normSum()).isNull();
		assertThat(household.housingCost()).isNull();
	}

	@Test
	void protectedIdentityFromAddressProtection() {
		when(integrationMock.getPerson(APPLICANT)).thenReturn(new PersonBasedPersonDTO().addressProtection(true));

		assertThat(service().hasProtectedIdentity(APPLICANT)).isTrue();
	}

	@Test
	void protectedIdentityFromProtectedRegistration() {
		when(integrationMock.getPerson(APPLICANT)).thenReturn(new PersonBasedPersonDTO().protectedRegistration(true));

		assertThat(service().hasProtectedIdentity(APPLICANT)).isTrue();
	}

	@Test
	void notProtectedWhenFlagsUnsetOrAbsent() {
		when(integrationMock.getPerson(APPLICANT)).thenReturn(new PersonBasedPersonDTO());

		assertThat(service().hasProtectedIdentity(APPLICANT)).isFalse();
	}

	@Test
	void notProtectedWhenNoPersonRecord() {
		when(integrationMock.getPerson(APPLICANT)).thenReturn(null);

		assertThat(service().hasProtectedIdentity(APPLICANT)).isFalse();
	}

	@Test
	void toYearMonthHandlesVariousFormats() {
		assertThat(LifecareEbCaseService.toYearMonth("2026-06")).contains(YearMonth.of(2026, 6));
		assertThat(LifecareEbCaseService.toYearMonth("2026-06-15")).contains(YearMonth.of(2026, 6));
		assertThat(LifecareEbCaseService.toYearMonth("2026-06-15T08:30:00")).contains(YearMonth.of(2026, 6));
		assertThat(LifecareEbCaseService.toYearMonth("2026-06-15 08:30:00")).contains(YearMonth.of(2026, 6));
		assertThat(LifecareEbCaseService.toYearMonth(null)).isEmpty();
		assertThat(LifecareEbCaseService.toYearMonth("  ")).isEmpty();
		assertThat(LifecareEbCaseService.toYearMonth("2026")).isEmpty();
		assertThat(LifecareEbCaseService.toYearMonth("garbage")).isEmpty();
		assertThat(LifecareEbCaseService.toYearMonth("2026-13")).isEmpty();
	}
}
