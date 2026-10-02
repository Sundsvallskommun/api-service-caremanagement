package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.attachments.service.AttachmentService;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaChild;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaIncome;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPlanning;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationRuleFeederTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ApplicationRulesService applicationRulesServiceMock;

	@Mock
	private AttachmentService attachmentServiceMock;

	@InjectMocks
	private ApplicationRuleFeeder feeder;

	// ----------------------------------------------------------------------------------------------------------------
	// Decision_ansokanFragor
	// ----------------------------------------------------------------------------------------------------------------

	@Test
	void applicationQuestionWarningsAsksNothingForAnEmptyApplication() {
		when(attachmentServiceMock.applicationAttachmentsExist(ERRAND_ID)).thenReturn(false);
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "BILAGOR", "NEJ")).thenReturn(unflagged());

		final var warnings = feeder.applicationQuestionWarnings(MUNICIPALITY_ID, ERRAND_ID, FinancialAssistanceEntity.create());

		assertThat(warnings).isEmpty();
		// every other question is unanswered on an empty application → not asked at all
		verify(applicationRulesServiceMock).question(MUNICIPALITY_ID, "BILAGOR", "NEJ");
		verifyNoMoreInteractions(applicationRulesServiceMock);
	}

	@Test
	void applicationQuestionWarningsSubstitutesTheChildsNameForThePersonalNumberPlaceholder() {
		final var errand = FinancialAssistanceEntity.create()
			.withChildren(List.of(
				FaChild.create().withPartyId("child-1").withFirstName("Astrid").withLastName("Lindgren").withResidenceExtent("HALF_TIME"),
				FaChild.create().withPartyId("child-2").withFirstName("Emil").withResidenceExtent("FULL_TIME"),
				FaChild.create().withPartyId("child-3"))); // no extent → not asked

		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "BARN_BOENDE_OMFATTNING", "HALF_TIME"))
			.thenReturn(flagged("CHILD_NOT_FULL_TIME", "I ansökan har personen uppgett att barn med PERSONNUMMER inte bor heltid – kontrollera"));
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "BARN_BOENDE_OMFATTNING", "FULL_TIME")).thenReturn(unflagged());
		stubNoAttachments();

		final var warnings = feeder.applicationQuestionWarnings(MUNICIPALITY_ID, ERRAND_ID, errand);

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey, WarningService.WarningInput::message)
			.containsExactly(tuple("CHILD_NOT_FULL_TIME", "child-1",
				"I ansökan har personen uppgett att barn med Astrid Lindgren inte bor heltid – kontrollera"));
	}

	@Test
	void applicationQuestionWarningsFallsBackToThePartyIdWhenTheChildHasNoName() {
		final var errand = FinancialAssistanceEntity.create()
			.withChildren(List.of(FaChild.create().withPartyId("child-1").withResidenceExtent("OTHER")));

		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "BARN_BOENDE_OMFATTNING", "OTHER"))
			.thenReturn(flagged("CHILD_NOT_FULL_TIME", "barn med PERSONNUMMER"));
		stubNoAttachments();

		final var warnings = feeder.applicationQuestionWarnings(MUNICIPALITY_ID, ERRAND_ID, errand);

		assertThat(warnings.getFirst().message()).isEqualTo("barn med child-1");
	}

	@Test
	void applicationQuestionWarningsTranslatesBooleansToJaNej() {
		final var errand = FinancialAssistanceEntity.create()
			.withChildrenResidenceChanged(true)
			.withHousingChanged(false)
			.withHasPendingBenefits(true)
			.withHasAssets(false)
			.withStaysInMunicipality(false);

		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "BARN_BOENDE_ANDRAT", "JA"))
			.thenReturn(flagged("CHILDREN_RESIDENCE_CHANGED", "Barns boende har ändrats"));
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "BOENDESITUATION_ANDRAD", "NEJ")).thenReturn(unflagged());
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "NY_ERSATTNING_SOKT", "JA"))
			.thenReturn(flagged("PENDING_BENEFIT", "Har sökt ny ersättning"));
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "NYA_TILLGANGAR", "NEJ")).thenReturn(unflagged());
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "VISTELSE_SUNDSVALL", "NEJ"))
			.thenReturn(flagged("STAY_OUTSIDE_MUNICIPALITY", "Bor inte i Sundsvall"));
		stubNoAttachments();

		final var warnings = feeder.applicationQuestionWarnings(MUNICIPALITY_ID, ERRAND_ID, errand);

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey)
			.containsExactly(
				tuple("CHILDREN_RESIDENCE_CHANGED", "children-residence"),
				tuple("PENDING_BENEFIT", "pending-benefits"),
				tuple("STAY_OUTSIDE_MUNICIPALITY", "stay-municipality"));
	}

	@Test
	void applicationQuestionWarningsAsksEachDeclaredIncomeTypeOnce() {
		final var errand = FinancialAssistanceEntity.create()
			.withIncomes(List.of(
				FaIncome.create().withIncomeType("SALARY").withAmount(new BigDecimal("12000")),
				FaIncome.create().withIncomeType("SALARY").withAmount(new BigDecimal("3000")),
				FaIncome.create().withIncomeType("SWISH_DEPOSITS").withAmount(new BigDecimal("400")),
				FaIncome.create())); // no type → skipped

		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "INKOMST_TYP", "SALARY"))
			.thenReturn(flagged("SALARY_JOB_STIMULUS", "kontrollera om det är aktuellt med jobbstimulans"));
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "INKOMST_TYP", "SWISH_DEPOSITS")).thenReturn(unflagged());
		stubNoAttachments();

		final var warnings = feeder.applicationQuestionWarnings(MUNICIPALITY_ID, ERRAND_ID, errand);

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey)
			.containsExactly(tuple("SALARY_JOB_STIMULUS", "SALARY"));
		verify(applicationRulesServiceMock).question(MUNICIPALITY_ID, "INKOMST_TYP", "SALARY");
		verify(applicationRulesServiceMock).question(MUNICIPALITY_ID, "INKOMST_TYP", "SWISH_DEPOSITS");
	}

	@Test
	void applicationQuestionWarningsFillsEachPlanningPlaceholder() {
		final var errand = FinancialAssistanceEntity.create().withPlannings(List.of(
			FaPlanning.create().withPerson("APPLICANT").withPlanningType("WORK").withWorkExtent("PART"),
			FaPlanning.create().withPerson("CO_APPLICANT").withPlanningType("SICK_LEAVE").withSickLeaveLevel("75"),
			FaPlanning.create().withPerson("APPLICANT").withPlanningType("SFI").withSfiStudyPath("2").withSfiCourse("C"),
			FaPlanning.create().withPlanningType("OTHER").withOtherDescription("praktik via AF"),
			FaPlanning.create())); // no type → skipped

		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "PLANERING", "WORK"))
			.thenReturn(flagged("PLANNING_REVIEW", "NAMN har uppgett planering arbete med omfattning HELTID/DELTID – kontrollera planeringen"));
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "PLANERING", "SICK_LEAVE"))
			.thenReturn(flagged("PLANNING_REVIEW", "NAMN har uppgett planering sjukskriven med sjukskrivningsgrad HELTID 100%/DELTID 75%/50%/25% – kontrollera planeringen"));
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "PLANERING", "SFI"))
			.thenReturn(flagged("PLANNING_REVIEW", "NAMN har uppgett planering SFI-studerande med studieväg 1/2/3 och kurs A/B/C/D – kontrollera planeringen"));
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "PLANERING", "OTHER"))
			.thenReturn(flagged("PLANNING_REVIEW", "NAMN har uppgett annan planering med texten XX – kontrollera planeringen"));
		stubNoAttachments();

		final var warnings = feeder.applicationQuestionWarnings(MUNICIPALITY_ID, ERRAND_ID, errand);

		assertThat(warnings).extracting(WarningService.WarningInput::sourceKey, WarningService.WarningInput::message)
			.containsExactly(
				tuple("Sökande:WORK", "Sökande har uppgett planering arbete med omfattning deltid – kontrollera planeringen"),
				tuple("Medsökande:SICK_LEAVE", "Medsökande har uppgett planering sjukskriven med sjukskrivningsgrad 75% – kontrollera planeringen"),
				tuple("Sökande:SFI", "Sökande har uppgett planering SFI-studerande med studieväg 2 och kurs C – kontrollera planeringen"),
				tuple("Sökande:OTHER", "Sökande har uppgett annan planering med texten praktik via AF – kontrollera planeringen"));
	}

	@Test
	void applicationQuestionWarningsLeavesAPlanningPlaceholderWithNoValueAlone() {
		final var errand = FinancialAssistanceEntity.create()
			.withPlannings(List.of(FaPlanning.create().withPerson("APPLICANT").withPlanningType("WORK")));

		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "PLANERING", "WORK"))
			.thenReturn(flagged("PLANNING_REVIEW", "NAMN har uppgett planering arbete med omfattning HELTID/DELTID – kontrollera"));
		stubNoAttachments();

		final var warnings = feeder.applicationQuestionWarnings(MUNICIPALITY_ID, ERRAND_ID, errand);

		assertThat(warnings.getFirst().message()).isEqualTo("Sökande har uppgett planering arbete med omfattning HELTID/DELTID – kontrollera");
	}

	@Test
	void applicationQuestionWarningsAsksThePaymentMethodPerPersonThatAnswered() {
		final var errand = FinancialAssistanceEntity.create().withPersons(List.of(
			FaPerson.create().withRole("APPLICANT").withPaymentSameAsPrevious(false),
			FaPerson.create().withRole("CO_APPLICANT").withPaymentSameAsPrevious(true),
			FaPerson.create().withRole("APPLICANT"))); // unanswered → not asked

		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "SAMMA_UTBETALNINGSSATT", "NEJ"))
			.thenReturn(flagged("PAYMENT_METHOD_CHANGED", "inte samma utbetalningssätt"));
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "SAMMA_UTBETALNINGSSATT", "JA")).thenReturn(unflagged());
		stubNoAttachments();

		final var warnings = feeder.applicationQuestionWarnings(MUNICIPALITY_ID, ERRAND_ID, errand);

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey)
			.containsExactly(tuple("PAYMENT_METHOD_CHANGED", "Sökande"));
	}

	@Test
	void applicationQuestionWarningsFlagsApplicationAttachments() {
		when(attachmentServiceMock.applicationAttachmentsExist(ERRAND_ID)).thenReturn(true);
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "BILAGOR", "JA"))
			.thenReturn(flagged("ATTACHMENTS_PRESENT", "I ansökan finns det bifogade bilagor – kontrollera bilagorna"));

		final var warnings = feeder.applicationQuestionWarnings(MUNICIPALITY_ID, ERRAND_ID, FinancialAssistanceEntity.create());

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey)
			.containsExactly(tuple("ATTACHMENTS_PRESENT", "attachments"));
	}

	@Test
	void applicationQuestionWarningsSkipsTheAttachmentRuleWhenTheListingFails() {
		when(attachmentServiceMock.applicationAttachmentsExist(ERRAND_ID)).thenThrow(new RuntimeException("attachments down"));

		final var warnings = feeder.applicationQuestionWarnings(MUNICIPALITY_ID, ERRAND_ID, FinancialAssistanceEntity.create());

		assertThat(warnings).isEmpty();
		verifyNoInteractions(applicationRulesServiceMock);
	}

	@Test
	void applicationQuestionWarningsFallsBackToTheGenericTypeAndTextWhenTheTableNamesNeither() {
		when(attachmentServiceMock.applicationAttachmentsExist(ERRAND_ID)).thenReturn(true);
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "BILAGOR", "JA"))
			.thenReturn(new ApplicationRulesService.RuleVerdict(true, "  ", null));

		final var warnings = feeder.applicationQuestionWarnings(MUNICIPALITY_ID, ERRAND_ID, FinancialAssistanceEntity.create());

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::message)
			.containsExactly(tuple(WarningService.TYPE_APPLICATION_REVIEW, "Kontrollera uppgiften i ansökan"));
	}

	// ----------------------------------------------------------------------------------------------------------------
	// Decision_inkomstMotForegaende
	// ----------------------------------------------------------------------------------------------------------------

	@Test
	void incomeComparisonWarningsSkipsATypeNeitherSideCarries() {
		final var warnings = feeder.incomeComparisonWarnings(MUNICIPALITY_ID, FinancialAssistanceEntity.create(), Map.of());

		assertThat(warnings).isEmpty();
		verifyNoInteractions(applicationRulesServiceMock);
	}

	@Test
	void incomeComparisonWarningsToleratesNullPreviousAmounts() {
		final var warnings = feeder.incomeComparisonWarnings(MUNICIPALITY_ID, FinancialAssistanceEntity.create(), null);

		assertThat(warnings).isEmpty();
		verifyNoInteractions(applicationRulesServiceMock);
	}

	@Test
	void incomeComparisonWarningsFlagsAnIncomeThePreviousCalculationHadAndTheApplicationLacks() {
		when(applicationRulesServiceMock.incomeAgainstPrevious(MUNICIPALITY_ID, "SALARY", true, false, new BigDecimal("15500"), null))
			.thenReturn(flagged("INCOME_MISSING_VS_PREVIOUS_CALCULATION", "Det fanns lön i föregående normberäkning"));

		final var warnings = feeder.incomeComparisonWarnings(MUNICIPALITY_ID, FinancialAssistanceEntity.create(),
			Map.of("SALARY", new BigDecimal("15500")));

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey, WarningService.WarningInput::message)
			.containsExactly(tuple("INCOME_MISSING_VS_PREVIOUS_CALCULATION", "SALARY", "Det fanns lön i föregående normberäkning"));
	}

	@Test
	void incomeComparisonWarningsSumsTheApplicationsIncomesOfTheSameType() {
		final var errand = FinancialAssistanceEntity.create().withIncomes(List.of(
			FaIncome.create().withIncomeType("CHILD_SUPPORT").withAmount(new BigDecimal("1000")),
			FaIncome.create().withIncomeType("CHILD_SUPPORT").withAmount(new BigDecimal("600")),
			FaIncome.create().withIncomeType("CHILD_SUPPORT"))); // null amount counts as zero

		when(applicationRulesServiceMock.incomeAgainstPrevious(MUNICIPALITY_ID, "CHILD_SUPPORT", true, true,
			new BigDecimal("1500"), new BigDecimal("1600"))).thenReturn(flagged("INCOME_AMOUNT_MISMATCH_PREVIOUS_CALCULATION", "Underhållsbidraget skiljer"));

		final var warnings = feeder.incomeComparisonWarnings(MUNICIPALITY_ID, errand, Map.of("CHILD_SUPPORT", new BigDecimal("1500")));

		assertThat(warnings).extracting(WarningService.WarningInput::sourceKey).containsExactly("CHILD_SUPPORT");
	}

	@Test
	void incomeComparisonWarningsReportsAnApplicationOnlyIncomeWithNoPreviousSum() {
		final var errand = FinancialAssistanceEntity.create().withIncomes(List.of(
			FaIncome.create().withIncomeType("RENT_SHARE_FROM_CHILD").withAmount(new BigDecimal("2000"))));

		when(applicationRulesServiceMock.incomeAgainstPrevious(MUNICIPALITY_ID, "RENT_SHARE_FROM_CHILD", false, true, null, new BigDecimal("2000")))
			.thenReturn(flagged("INCOME_AMOUNT_MISMATCH_PREVIOUS_CALCULATION", "Hyresdel skiljer"));

		final var warnings = feeder.incomeComparisonWarnings(MUNICIPALITY_ID, errand, Map.of());

		assertThat(warnings).extracting(WarningService.WarningInput::sourceKey).containsExactly("RENT_SHARE_FROM_CHILD");
		verify(applicationRulesServiceMock).incomeAgainstPrevious(MUNICIPALITY_ID, "RENT_SHARE_FROM_CHILD", false, true, null, new BigDecimal("2000"));
		verifyNoMoreInteractions(applicationRulesServiceMock);
	}

	// ----------------------------------------------------------------------------------------------------------------
	// Decision_ansokanMotBerakning
	// ----------------------------------------------------------------------------------------------------------------

	@Test
	void previousCalculationWarningsReturnsEmptyWithoutAPreviousCalculation() {
		final var errand = FinancialAssistanceEntity.create().withHousingPersonCount(3);

		assertThat(feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, PreviousHousehold.empty())).isEmpty();
		assertThat(feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, null)).isEmpty();

		verifyNoInteractions(applicationRulesServiceMock);
	}

	@Test
	void previousCalculationWarningsComparesChildrenOnPartyIds() {
		final var errand = FinancialAssistanceEntity.create()
			.withHasChildrenUnder21(true)
			.withPersons(List.of(FaPerson.create().withRole("APPLICANT").withPartyId("adult-1")))
			.withChildren(List.of(FaChild.create().withPartyId("child-1")));

		// the previous calculation carries the applicant plus a child that is not in the application
		final var previous = new PreviousHousehold(Set.of("adult-1", "child-1", "child-2"), true, 3, null, null, null);

		when(applicationRulesServiceMock.againstPreviousCalculation(MUNICIPALITY_ID, "BARN_PERSONNUMMER", false))
			.thenReturn(flagged("CHILDREN_MISMATCH_PREVIOUS_CALCULATION", "Barns personnummer stämmer inte"));

		final var warnings = feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, previous);

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey, WarningService.WarningInput::message)
			.containsExactly(tuple("CHILDREN_MISMATCH_PREVIOUS_CALCULATION", "children", "Barns personnummer stämmer inte"));
	}

	@Test
	void previousCalculationWarningsMatchesPartyIdsRegardlessOfCase() {
		final var errand = FinancialAssistanceEntity.create()
			.withHasChildrenUnder21(true)
			.withPersons(List.of(FaPerson.create().withRole("APPLICANT").withPartyId("f47ac10b-58cc-4372-a567-0e02b2c3d479")))
			.withChildren(List.of(FaChild.create().withPartyId("c0ffee00-0000-4000-8000-00000000000a")));

		// the citizen register's upper-case spelling of the same two people
		final var previous = new PreviousHousehold(Set.of("F47AC10B-58CC-4372-A567-0E02B2C3D479", "C0FFEE00-0000-4000-8000-00000000000A"), true, 2, null, null, null);

		when(applicationRulesServiceMock.againstPreviousCalculation(MUNICIPALITY_ID, "BARN_PERSONNUMMER", true)).thenReturn(unflagged());

		assertThat(feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, previous)).isEmpty();
	}

	/**
	 * A previous household missing a member it could not name is short exactly one child, which would read as a
	 * mismatch that never happened. The comparison is skipped instead.
	 */
	@Test
	void previousCalculationWarningsSkipsTheChildrenComparisonWhenThePreviousHouseholdIsIncomplete() {
		final var errand = FinancialAssistanceEntity.create()
			.withHasChildrenUnder21(true)
			.withPersons(List.of(FaPerson.create().withRole("APPLICANT").withPartyId("adult-1")))
			.withChildren(List.of(FaChild.create().withPartyId("child-1")));

		final var previous = new PreviousHousehold(Set.of("adult-1"), false, 2, null, null, null);

		assertThat(feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, previous)).isEmpty();
	}

	@Test
	void previousCalculationWarningsSkipsTheChildrenComparisonWhenNoChildrenAreStated() {
		final var errand = FinancialAssistanceEntity.create()
			.withHasChildrenUnder21(false)
			.withChildren(List.of(FaChild.create().withPartyId("child-1")));
		final var previous = new PreviousHousehold(Set.of("adult-1"), true, 1, null, null, null);

		assertThat(feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, previous)).isEmpty();

		verify(applicationRulesServiceMock, never()).againstPreviousCalculation(anyString(), eq("BARN_PERSONNUMMER"), anyBoolean());
	}

	@Test
	void previousCalculationWarningsSkipsTheChildrenComparisonWhenAnAdultHasNoPartyId() {
		final var errand = FinancialAssistanceEntity.create()
			.withHasChildrenUnder21(true)
			.withPersons(List.of(FaPerson.create().withRole("APPLICANT")))
			.withChildren(List.of(FaChild.create().withPartyId("child-1")));
		final var previous = new PreviousHousehold(Set.of("child-1"), true, 1, null, null, null);

		assertThat(feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, previous)).isEmpty();

		verify(applicationRulesServiceMock, never()).againstPreviousCalculation(anyString(), eq("BARN_PERSONNUMMER"), anyBoolean());
	}

	@Test
	void previousCalculationWarningsSkipsTheChildrenComparisonWhenAChildHasNoPartyId() {
		final var errand = FinancialAssistanceEntity.create()
			.withHasChildrenUnder21(true)
			.withChildren(List.of(FaChild.create().withFirstName("Astrid")));
		final var previous = new PreviousHousehold(Set.of("adult-1"), true, 1, null, null, null);

		assertThat(feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, previous)).isEmpty();

		verifyNoInteractions(applicationRulesServiceMock);
	}

	@Test
	void previousCalculationWarningsFillsBothCountPlaceholders() {
		final var errand = FinancialAssistanceEntity.create().withHousingChanged(false).withHousingPersonCount(4);
		final var previous = new PreviousHousehold(Set.of("199001011234", "201801012380"), true, 2, null, null, null);

		when(applicationRulesServiceMock.againstPreviousCalculation(MUNICIPALITY_ID, "ANTAL_I_BOSTADEN", false))
			.thenReturn(flagged("HOUSEHOLD_COUNT_MISMATCH_PREVIOUS_CALCULATION",
				"I ansökan har personen uppgett att det bor ANTAL personer i bostaden och föregående beräkning visar ANTAL personer – kontrollera"));

		final var warnings = feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, previous);

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey, WarningService.WarningInput::message)
			.containsExactly(tuple("HOUSEHOLD_COUNT_MISMATCH_PREVIOUS_CALCULATION", "household-count",
				"I ansökan har personen uppgett att det bor 4 personer i bostaden och föregående beräkning visar 2 personer – kontrollera"));
	}

	@Test
	void previousCalculationWarningsSkipsTheCountComparisonWhenTheHousingSituationChanged() {
		final var errand = FinancialAssistanceEntity.create().withHousingChanged(true).withHousingPersonCount(4);
		final var previous = new PreviousHousehold(Set.of("adult-1"), true, 1, null, null, null);

		assertThat(feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, previous)).isEmpty();

		verify(applicationRulesServiceMock, never()).againstPreviousCalculation(anyString(), eq("ANTAL_I_BOSTADEN"), anyBoolean());
	}

	@Test
	void previousCalculationWarningsSkipsTheCountComparisonWhenTheApplicationStatesNoCount() {
		final var errand = FinancialAssistanceEntity.create().withHousingChanged(false);
		final var previous = new PreviousHousehold(Set.of("adult-1"), true, 1, null, null, null);

		assertThat(feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, previous)).isEmpty();

		verify(applicationRulesServiceMock, never()).againstPreviousCalculation(anyString(), eq("ANTAL_I_BOSTADEN"), anyBoolean());
	}

	@Test
	void previousCalculationWarningsReadsTheFreeTextNormAndFillsBothPlaceholders() {
		final var errand = FinancialAssistanceEntity.create().withNormType(List.of("OTHER_NORM"));
		final var previous = new PreviousHousehold(Set.of("adult-1"), true, 1, null, null, " Riksnorm 2026 ");

		when(applicationRulesServiceMock.againstPreviousCalculation(MUNICIPALITY_ID, "NORM", false))
			.thenReturn(flagged("NORM_MISMATCH_PREVIOUS_CALCULATION",
				"I ansökan har personen uppgett NORM och föregående beräkning visar NORM – kontrollera och ändra eventuellt norm"));

		final var warnings = feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, previous);

		assertThat(warnings).extracting(WarningService.WarningInput::type, WarningService.WarningInput::sourceKey, WarningService.WarningInput::message)
			.containsExactly(tuple("NORM_MISMATCH_PREVIOUS_CALCULATION", "norm",
				"I ansökan har personen uppgett Annan norm och föregående beräkning visar Riksnorm 2026 – kontrollera och ändra eventuellt norm"));
	}

	@Test
	void previousCalculationWarningsReadsAnUnrecognisedFreeTextNormAsAnotherNorm() {
		final var errand = FinancialAssistanceEntity.create().withNormType(List.of("OTHER_NORM"));
		final var previous = new PreviousHousehold(Set.of("adult-1"), true, 1, null, null, "Norm för eget boende");

		when(applicationRulesServiceMock.againstPreviousCalculation(MUNICIPALITY_ID, "NORM", true)).thenReturn(unflagged());

		assertThat(feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, previous)).isEmpty();
	}

	@Test
	void previousCalculationWarningsPrefersTheNationalNormWhenBothAreTicked() {
		final var errand = FinancialAssistanceEntity.create().withNormType(List.of("OTHER_NORM", "NATIONAL_NORM"));
		final var previous = new PreviousHousehold(Set.of("adult-1"), true, 1, null, null, "Riksnorm");

		when(applicationRulesServiceMock.againstPreviousCalculation(MUNICIPALITY_ID, "NORM", true)).thenReturn(unflagged());

		assertThat(feeder.previousCalculationWarnings(MUNICIPALITY_ID, errand, previous)).isEmpty();
	}

	@Test
	void previousCalculationWarningsSkipsTheNormComparisonWhenEitherSideIsBlank() {
		final var withoutPreviousNorm = new PreviousHousehold(Set.of("adult-1"), true, 1, null, null, "   ");
		final var withPreviousNorm = new PreviousHousehold(Set.of("adult-1"), true, 1, null, null, "Riksnorm");

		assertThat(feeder.previousCalculationWarnings(MUNICIPALITY_ID,
			FinancialAssistanceEntity.create().withNormType(List.of("NATIONAL_NORM")), withoutPreviousNorm)).isEmpty();
		assertThat(feeder.previousCalculationWarnings(MUNICIPALITY_ID, FinancialAssistanceEntity.create(), withPreviousNorm)).isEmpty();

		verify(applicationRulesServiceMock, never()).againstPreviousCalculation(anyString(), eq("NORM"), anyBoolean());
	}

	private void stubNoAttachments() {
		when(attachmentServiceMock.applicationAttachmentsExist(ERRAND_ID)).thenReturn(false);
		when(applicationRulesServiceMock.question(MUNICIPALITY_ID, "BILAGOR", "NEJ")).thenReturn(unflagged());
	}

	private static ApplicationRulesService.RuleVerdict flagged(final String code, final String rule) {
		return new ApplicationRulesService.RuleVerdict(true, code, rule);
	}

	private static ApplicationRulesService.RuleVerdict unflagged() {
		return ApplicationRulesService.RuleVerdict.none();
	}
}
