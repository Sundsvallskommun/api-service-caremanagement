package se.sundsvall.caremanagement.lifecare.service;

import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefamilycare.ApiPaginationCompositePersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefamilycare.CommonCalculationExpenseDTO;
import generated.se.sundsvall.lifecarefamilycare.CommonCalculationIncomeDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedAktualiseringDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedCalculationPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedDecisionDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedDecisionPersonDTO;
import generated.se.sundsvall.lifecarefamilycare.PersonBasedPersonDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.lifecare.integration.LifecareFamilyCareIntegration;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousFamily;

import static java.time.Month.JUNE;
import static java.time.Month.MARCH;
import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LifecareCaseServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	private static final String APPLICANT = "198001012389";
	private static final LocalDate REFERENCE = LocalDate.of(2026, JUNE, 15);

	// The direct FamilyCare route: it answers with personal identity numbers, so the roster resolves them to party ids.
	private static final String APPLICANT_PARTY_ID = "3f5ca9a0-1c2d-4e3f-8a9b-0c1d2e3f4a5b";
	private static final String CO_APPLICANT = "198202022397";
	private static final String CO_APPLICANT_PARTY_ID = "7b6a5c4d-3e2f-4a1b-9c8d-7e6f5a4b3c2d";
	private static final String CHILD = "201801012380";
	private static final String CHILD_PARTY_ID = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d";

	@Mock
	private LifecareFamilyCareIntegration integrationMock;

	@Mock
	private CitizenService citizenServiceMock;

	private LifecareCaseService service() {
		return new LifecareCaseService(integrationMock, citizenServiceMock, 13, List.of("Aktuell"));
	}

	private void noActualisations() {
		when(integrationMock.getActualisations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO());
	}

	private void noCalculations() {
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO());
	}

	@Test
	void footprintFromDecisionsWithMonthRangeAndCoApplicant() {
		final var decision = new PersonBasedDecisionDTO()
			.fromDate("2026-05-01")
			.toDate("2026-06-30")
			.addDecisionPersonDTOsItem(new PersonBasedDecisionPersonDTO().personId("198202022397").isCoApplicant(true));

		noActualisations();
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO().addResultItem(decision));
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().addResultItem(new PersonBasedCalculationDTO()));

		final var summary = service().summarize(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE);

		assertThat(summary.hasFootprint()).isTrue();
		assertThat(summary.decisionMonths()).containsExactlyInAnyOrder(YearMonth.of(2026, MAY), YearMonth.of(2026, JUNE));
		assertThat(summary.latestDecisionPeriod()).isEqualTo(YearMonth.of(2026, JUNE));
		assertThat(summary.hasCalculation()).isTrue();
		assertThat(summary.hasCoApplicant()).isTrue();
	}

	@Test
	void footprintFromActualisationOnly() {
		when(integrationMock.getActualisations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO()
				.addResultItem(new PersonBasedAktualiseringDTO().status("Aktuell")));
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO());
		noCalculations();

		final var summary = service().summarize(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE);

		assertThat(summary.hasFootprint()).isTrue();
		assertThat(summary.hasOpenCase()).isTrue();
		assertThat(summary.decisionMonths()).isEmpty();
		assertThat(summary.latestDecisionPeriod()).isNull();
		assertThat(summary.hasCoApplicant()).isFalse();
	}

	@Test
	void openCaseStatusMatchIsCaseAndWhitespaceInsensitive() {
		when(integrationMock.getActualisations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO()
				.addResultItem(new PersonBasedAktualiseringDTO().status("  aKtUeLl ")));
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO());
		noCalculations();

		assertThat(service().summarize(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE).hasOpenCase()).isTrue();
	}

	@Test
	void aStatusOutsideTheOpenVocabularyIsNotAnOpenCase() {
		when(integrationMock.getActualisations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO()
				.addResultItem(new PersonBasedAktualiseringDTO().status("Avslutad")));
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO());
		noCalculations();

		final var summary = service().summarize(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE);

		assertThat(summary.hasFootprint()).isTrue();
		assertThat(summary.hasOpenCase()).isFalse();
	}

	@Test
	void anActualisationWithoutAReadableStatusLeavesTheOpenCaseUnknown() {
		// FamilyCare's status vocabulary is not fully confirmed — an unreadable status must report "unknown", not "closed".
		when(integrationMock.getActualisations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO()
				.addResultItem(new PersonBasedAktualiseringDTO().status(" ")));
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO());
		noCalculations();

		final var summary = service().summarize(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE);

		assertThat(summary.hasFootprint()).isTrue();
		assertThat(summary.hasOpenCase()).isNull();
	}

	@Test
	void blankConfiguredStatusesAreIgnored() {
		when(integrationMock.getActualisations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedAktualiseringDTO()
				.addResultItem(new PersonBasedAktualiseringDTO().status("Aktuell")));
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO());
		noCalculations();

		final var service = new LifecareCaseService(integrationMock, citizenServiceMock, 13, List.of(" ", "Aktuell"));

		assertThat(service.summarize(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE).hasOpenCase()).isTrue();
	}

	@Test
	void footprintFromCalculationOnly() {
		noActualisations();
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO());
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().addResultItem(new PersonBasedCalculationDTO()));

		final var summary = service().summarize(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE);

		assertThat(summary.hasFootprint()).isTrue();
		assertThat(summary.hasCalculation()).isTrue();
	}

	@Test
	void noFootprintYieldsEmptySummary() {
		noActualisations();
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO());
		noCalculations();

		final var summary = service().summarize(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE);

		assertThat(summary.hasFootprint()).isFalse();
		assertThat(summary.decisionMonths()).isEmpty();
		assertThat(summary.latestDecisionPeriod()).isNull();
		assertThat(summary.hasCalculation()).isFalse();
		assertThat(summary.hasCoApplicant()).isFalse();
	}

	@Test
	void nullCompositesAreToleratedAsEmpty() {
		when(integrationMock.getActualisations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(null);
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(null);
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(null);

		final var summary = service().summarize(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE);

		assertThat(summary.hasFootprint()).isFalse();
		assertThat(summary.decisionMonths()).isEmpty();
	}

	@Test
	void decisionMonthsFromSingleDateAndCoApplicantScalar() {
		noActualisations();
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO()
				.addResultItem(new PersonBasedDecisionDTO().toDate("2026-06-30").coApplicant("198202022397")));
		noCalculations();

		final var summary = service().summarize(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE);

		assertThat(summary.decisionMonths()).containsExactly(YearMonth.of(2026, JUNE));
		assertThat(summary.hasCoApplicant()).isTrue();
	}

	@Test
	void latestDecisionDrivesPeriodAndConstellation() {
		final var older = new PersonBasedDecisionDTO().toDate("2026-03-31").coApplicant("197001010000");
		final var newer = new PersonBasedDecisionDTO().toDate("2026-05-31"); // no co-applicant
		noActualisations();
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO().result(List.of(older, newer)));
		noCalculations();

		final var summary = service().summarize(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE);

		assertThat(summary.latestDecisionPeriod()).isEqualTo(YearMonth.of(2026, MAY));
		assertThat(summary.hasCoApplicant()).isFalse(); // newest decision has none
		assertThat(summary.decisionMonths()).containsExactlyInAnyOrder(YearMonth.of(2026, MARCH), YearMonth.of(2026, MAY));
	}

	@Test
	void latestRosterReadsLatestCalculationMembersAndFlaggedCoApplicant() {
		final var olderCalc = new PersonBasedCalculationDTO().toDate("2026-04-30")
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId("198001019999").name("Old"));
		final var newerCalc = new PersonBasedCalculationDTO().toDate("2026-06-30")
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(APPLICANT).name("Anna"))
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(CHILD).name("Kid"));
		final var decision = new PersonBasedDecisionDTO().toDate("2026-06-30")
			.addDecisionPersonDTOsItem(new PersonBasedDecisionPersonDTO().personId(CO_APPLICANT).isCoApplicant(true));

		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, APPLICANT)).thenReturn(Optional.of(APPLICANT_PARTY_ID));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, CO_APPLICANT)).thenReturn(Optional.of(CO_APPLICANT_PARTY_ID));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, CHILD)).thenReturn(Optional.of(CHILD_PARTY_ID));

		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of(olderCalc, newerCalc)));
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO().addResultItem(decision));

		final var roster = service().latestRoster(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE);

		assertThat(roster.applicant()).isEqualTo(APPLICANT_PARTY_ID);
		assertThat(roster.coApplicant()).isEqualTo(CO_APPLICANT_PARTY_ID);
		assertThat(roster.members()).extracting(LifecareRoster.Member::partyId).containsExactly(APPLICANT_PARTY_ID, CHILD_PARTY_ID);
		assertThat(roster.members()).extracting(LifecareRoster.Member::name).containsExactly("Anna", "Kid");
	}

	@Test
	void latestRosterFiltersBlankPersonIdsAndFallsBackToScalarCoApplicant() {
		final var calc = new PersonBasedCalculationDTO().toDate("2026-06-30")
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(APPLICANT).name("Anna"))
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId("  ").name("Blank"));

		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().addResultItem(calc));
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO()
				.addResultItem(new PersonBasedDecisionDTO().toDate("2026-06-30").coApplicant("Bo Berg")));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, APPLICANT)).thenReturn(Optional.of(APPLICANT_PARTY_ID));

		final var roster = service().latestRoster(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE);

		assertThat(roster.members()).extracting(LifecareRoster.Member::partyId).containsExactly(APPLICANT_PARTY_ID);
		// FamilyCare's own co-applicant field is free text, not an identity, so it is passed through as it came.
		assertThat(roster.coApplicant()).isEqualTo("Bo Berg");
	}

	@Test
	void latestRosterEmptyWhenNoCalculationsOrDecisions() {
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO());
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedDecisionDTO());

		final var roster = service().latestRoster(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE);

		// the applicant is the argument itself — already a party id, so no citizen lookup
		assertThat(roster.applicant()).isEqualTo(APPLICANT_PARTY_ID);
		assertThat(roster.coApplicant()).isNull();
		verifyNoInteractions(citizenServiceMock);
		assertThat(roster.members()).isEmpty();
	}

	@Test
	void latestRosterToleratesNullComposites() {
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(null);
		when(integrationMock.getDecisions(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(null);

		final var roster = service().latestRoster(MUNICIPALITY_ID, APPLICANT_PARTY_ID, REFERENCE);

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
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of(older, previous, thisMonth)));

		final var types = service().previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE));

		assertThat(types).containsExactlyInAnyOrder("Bostadsbidrag", "Dagersättning");
	}

	@Test
	void previousCalculationIncomeTypesEmptyWhenNoPriorCalc() {
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(null);

		assertThat(service().previousCalculationIncomeTypes(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE))).isEmpty();
	}

	@Test
	void previousFamilyCarriesTheMembersTheirDeviationsAndTheCommonHouseholdCost() {
		final var previous = new PersonBasedCalculationDTO().toDate("2026-05-31")
			.commonHouseholdCost(1234.0)
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(APPLICANT_PARTY_ID).name("NILSSON KARIN"))
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(CHILD_PARTY_ID).name("NILSSON OLLE")
				.deviationFromDate("2026-05-01T00:00:00").deviationToDate("2026-05-15"))
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(" ")); // blank filtered out
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of(previous)));
		when(integrationMock.respondsWithPartyId()).thenReturn(true);

		final var family = service().previousFamily(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE));

		assertThat(family.complete()).isTrue();
		assertThat(family.commonHouseholdCost()).isEqualByComparingTo("1234");
		assertThat(family.members()).containsExactly(
			new PreviousFamily.Member(APPLICANT_PARTY_ID, "NILSSON KARIN", null, null),
			new PreviousFamily.Member(CHILD_PARTY_ID, "NILSSON OLLE", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15)));
		assertThat(family.members().get(1).hasDeviation()).isTrue();
		assertThat(family.members().getFirst().hasDeviation()).isFalse();
	}

	@Test
	void previousFamilyIsIncompleteWhenAMemberDoesNotResolve() {
		final var previous = new PersonBasedCalculationDTO().toDate("2026-05-31")
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(APPLICANT).deviationFromDate("not a date"))
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId("201801012380"));
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of(previous)));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, APPLICANT)).thenReturn(Optional.of(APPLICANT_PARTY_ID));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, "201801012380")).thenReturn(Optional.empty());

		final var family = service().previousFamily(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE));

		assertThat(family.complete()).isFalse();
		assertThat(family.commonHouseholdCost()).isNull();
		assertThat(family.members()).extracting(PreviousFamily.Member::partyId).containsExactly(APPLICANT_PARTY_ID, null);
		assertThat(family.members().getFirst().deviationFrom()).isNull(); // unreadable date
	}

	@Test
	void previousFamilyIsEmptyWithoutAPreviousCalculation() {
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of()));

		final var family = service().previousFamily(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE));

		assertThat(family.isEmpty()).isTrue();
		assertThat(family).isEqualTo(PreviousFamily.empty());
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
			.addCalculationExpensesDTOsItem(new CommonCalculationExpenseDTO().type("Electricity").approvedAmount(900.0)) // not housing
			.norm("Riksnorm 2026");
		final var current = new PersonBasedCalculationDTO().toDate("2026-06-30"); // not strictly before June -> excluded
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of(older, previous, current)));
		// The direct route answers with personal identity numbers, so each member is resolved to its party id.
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, APPLICANT)).thenReturn(Optional.of(APPLICANT_PARTY_ID));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, CHILD)).thenReturn(Optional.of(CHILD_PARTY_ID));

		final var household = service().previousHousehold(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE));

		assertThat(household.partyIds()).containsExactlyInAnyOrder(APPLICANT_PARTY_ID, CHILD_PARTY_ID);
		assertThat(household.partyIdsComplete()).isTrue();
		assertThat(household.memberCount()).isEqualTo(2);
		assertThat(household.normSum()).isEqualTo(BigDecimal.valueOf(12345.0));
		assertThat(household.housingCost()).isEqualTo(BigDecimal.valueOf(7500.0)); // 6000 (approved) + 1500 (applied fallback)
		assertThat(household.norm()).isEqualTo("Riksnorm 2026");
	}

	/**
	 * The integrator answers with party ids — the same identity the errand's members carry — so the previous household
	 * is passed through as it is, with no citizen lookup at all.
	 */
	@Test
	void previousHouseholdPassesPartyIdsThroughOnTheIntegratorRoute() {
		final var previous = new PersonBasedCalculationDTO().toDate("2026-05-31")
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(APPLICANT_PARTY_ID))
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(CHILD_PARTY_ID));
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of(previous)));
		when(integrationMock.respondsWithPartyId()).thenReturn(true);

		final var household = service().previousHousehold(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE));

		assertThat(household.partyIds()).containsExactlyInAnyOrder(APPLICANT_PARTY_ID, CHILD_PARTY_ID);
		assertThat(household.partyIdsComplete()).isTrue();
		assertThat(household.memberCount()).isEqualTo(2);
		verifyNoInteractions(citizenServiceMock);
	}

	/**
	 * A member that does not resolve leaves the household short of someone it cannot name. The count still reports the
	 * calculation's own two members — that comparison stays valid — but the set is flagged incomplete so the member
	 * comparison is skipped rather than reported as a difference that was never there.
	 */
	@Test
	void previousHouseholdIsIncompleteWhenAMemberDoesNotResolve() {
		final var previous = new PersonBasedCalculationDTO().toDate("2026-05-31")
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(APPLICANT))
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(CHILD));
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of(previous)));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, APPLICANT)).thenReturn(Optional.of(APPLICANT_PARTY_ID));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, CHILD)).thenReturn(Optional.empty());

		final var household = service().previousHousehold(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE));

		assertThat(household.partyIds()).containsExactly(APPLICANT_PARTY_ID);
		assertThat(household.partyIdsComplete()).isFalse();
		assertThat(household.memberCount()).isEqualTo(2);
	}

	/**
	 * The direct route answers with personal identity numbers, so the amounts have to be resolved to the party ids
	 * careM's own draft person rows are keyed by — otherwise the Belopp column never matches a single member.
	 */
	@Test
	void previousPersonAmountsKeyedByPartyIdOnTheDirectRoute() {
		final var older = new PersonBasedCalculationDTO().toDate("2026-03-31")
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(APPLICANT).amount(999.0));
		final var previous = new PersonBasedCalculationDTO().toDate("2026-05-31")
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(APPLICANT).amount(1431.0))
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(CHILD).amount(2100.0))
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(CO_APPLICANT)) // no amount -> skipped
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId("198001019999").amount(50.0)); // unresolvable -> skipped
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of(older, previous)));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, APPLICANT)).thenReturn(Optional.of(APPLICANT_PARTY_ID));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, CHILD)).thenReturn(Optional.of(CHILD_PARTY_ID));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, CO_APPLICANT)).thenReturn(Optional.of(CO_APPLICANT_PARTY_ID));
		when(citizenServiceMock.getPartyId(MUNICIPALITY_ID, "198001019999")).thenReturn(Optional.empty());

		final var amounts = service().previousPersonAmounts(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE));

		assertThat(amounts).hasSize(2)
			.containsEntry(APPLICANT_PARTY_ID, BigDecimal.valueOf(1431.0))
			.containsEntry(CHILD_PARTY_ID, BigDecimal.valueOf(2100.0));
	}

	/** The integrator already answers with party ids, so nothing is resolved. */
	@Test
	void previousPersonAmountsPassesPartyIdsThroughOnTheIntegratorRoute() {
		final var previous = new PersonBasedCalculationDTO().toDate("2026-05-31")
			.addCalculationPersonDTOsItem(new PersonBasedCalculationPersonDTO().personId(APPLICANT_PARTY_ID).amount(1431.0));
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of(previous)));
		when(integrationMock.respondsWithPartyId()).thenReturn(true);

		final var amounts = service().previousPersonAmounts(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE));

		assertThat(amounts).containsExactly(entry(APPLICANT_PARTY_ID, BigDecimal.valueOf(1431.0)));
		verifyNoInteractions(citizenServiceMock);
	}

	@Test
	void previousPersonAmountsEmptyWhenNoPriorCalculation() {
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(null);

		assertThat(service().previousPersonAmounts(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE))).isEmpty();
	}

	@Test
	void previousHouseholdEmptyWhenNoPriorCalculation() {
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(null);

		final var household = service().previousHousehold(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE));

		assertThat(household.partyIds()).isEmpty();
		assertThat(household.memberCount()).isZero();
		assertThat(household.normSum()).isNull();
		assertThat(household.housingCost()).isNull();
		assertThat(household.norm()).isNull();
	}

	@Test
	void previousCalculationIncomeAmountsSumsBothSidesPerMappedType() {
		final var older = new PersonBasedCalculationDTO().toDate("2026-03-31")
			.addCalculationIncomesDTOsItem(new CommonCalculationIncomeDTO().type("Lön efter skatt").amountApplicant(999.0));
		final var previous = new PersonBasedCalculationDTO().toDate("2026-05-31")
			.addCalculationIncomesDTOsItem(new CommonCalculationIncomeDTO().type("Lön efter skatt").amountApplicant(12000.0).amountCoApplicant(3000.0))
			.addCalculationIncomesDTOsItem(new CommonCalculationIncomeDTO().type("Lön efter skatt").amountApplicant(500.0)) // same careM type -> merged
			.addCalculationIncomesDTOsItem(new CommonCalculationIncomeDTO().type("Pension/SA/Livränta/Omvårdnadsbidrag").amountCoApplicant(1800.0)) // applicant side null -> 0
			.addCalculationIncomesDTOsItem(new CommonCalculationIncomeDTO().type("Underhållsstöd").amountApplicant(1500.0))
			.addCalculationIncomesDTOsItem(new CommonCalculationIncomeDTO().type("Barnbidrag/Flerbarnstillägg").amountApplicant(1250.0)) // unmapped
			.addCalculationIncomesDTOsItem(new CommonCalculationIncomeDTO().type("Barnpension").amountApplicant(2000.0)); // never a pension insurance
		final var current = new PersonBasedCalculationDTO().toDate("2026-06-30"); // not strictly before June
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any()))
			.thenReturn(new ApiPaginationCompositePersonBasedCalculationDTO().result(List.of(older, previous, current)));

		final var amounts = service().previousCalculationIncomeAmounts(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE));

		assertThat(amounts).containsOnlyKeys("SALARY", "OCCUPATIONAL_PENSION_INSURANCE", "CHILD_SUPPORT");
		assertThat(amounts.get("SALARY")).isEqualByComparingTo("15500");
		assertThat(amounts.get("OCCUPATIONAL_PENSION_INSURANCE")).isEqualByComparingTo("1800");
		assertThat(amounts.get("CHILD_SUPPORT")).isEqualByComparingTo("1500");
	}

	@Test
	void previousCalculationIncomeAmountsEmptyWhenNoPriorCalculation() {
		when(integrationMock.getCalculations(eq(MUNICIPALITY_ID), eq(APPLICANT_PARTY_ID), any(), any())).thenReturn(null);

		assertThat(service().previousCalculationIncomeAmounts(MUNICIPALITY_ID, APPLICANT_PARTY_ID, YearMonth.of(2026, JUNE))).isEmpty();
	}

	@Test
	void protectedIdentityFromAddressProtection() {
		when(integrationMock.getPerson(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(new PersonBasedPersonDTO().addressProtection(true));

		assertThat(service().hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).isTrue();
	}

	@Test
	void protectedIdentityFromProtectedRegistration() {
		when(integrationMock.getPerson(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(new PersonBasedPersonDTO().protectedRegistration(true));

		assertThat(service().hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).isTrue();
	}

	@Test
	void notProtectedWhenFlagsUnsetOrAbsent() {
		when(integrationMock.getPerson(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(new PersonBasedPersonDTO());

		assertThat(service().hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).isFalse();
	}

	@Test
	void notProtectedWhenNoPersonRecord() {
		when(integrationMock.getPerson(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).thenReturn(null);

		assertThat(service().hasProtectedIdentity(MUNICIPALITY_ID, APPLICANT_PARTY_ID)).isFalse();
	}

	@Test
	void toYearMonthHandlesVariousFormats() {
		assertThat(LifecareCaseService.toYearMonth("2026-06")).contains(YearMonth.of(2026, JUNE));
		assertThat(LifecareCaseService.toYearMonth("2026-06-15")).contains(YearMonth.of(2026, JUNE));
		assertThat(LifecareCaseService.toYearMonth("2026-06-15T08:30:00")).contains(YearMonth.of(2026, JUNE));
		assertThat(LifecareCaseService.toYearMonth("2026-06-15 08:30:00")).contains(YearMonth.of(2026, JUNE));
		assertThat(LifecareCaseService.toYearMonth(null)).isEmpty();
		assertThat(LifecareCaseService.toYearMonth("  ")).isEmpty();
		assertThat(LifecareCaseService.toYearMonth("2026")).isEmpty();
		assertThat(LifecareCaseService.toYearMonth("garbage")).isEmpty();
		assertThat(LifecareCaseService.toYearMonth("2026-13")).isEmpty();
	}
}
