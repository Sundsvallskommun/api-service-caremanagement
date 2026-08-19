package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.service.model.FamilyCareIncomeLine;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaChild;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCost;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ORIGIN_SYSTEM;
import static se.sundsvall.caremanagement.types.financialassistance.service.CalculationConstants.ROLE_CHILD;

@ExtendWith(MockitoExtension.class)
class CalculationFeederTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ExpenseRulesService expenseRulesServiceMock;

	@Mock
	private RenewalDeltaService renewalDeltaServiceMock;

	@InjectMocks
	private CalculationFeeder feeder;

	@Test
	void incomeRowsMapsEachLine() {
		final var date = OffsetDateTime.parse("2026-05-15T00:00:00Z");
		final var lines = List.of(
			new FamilyCareIncomeLine(20, "Bostadsbidrag", "APPLICANT", new BigDecimal("1850"), date, "note"),
			new FamilyCareIncomeLine(21, "Lön", "CO_APPLICANT", new BigDecimal("12000"), date, null));

		final var rows = feeder.incomeRows(ERRAND_ID, lines);

		assertThat(rows).hasSize(2)
			.allMatch(r -> ERRAND_ID.equals(r.getErrandId()) && ORIGIN_SYSTEM.equals(r.getOrigin()));
		final var first = rows.getFirst();
		assertThat(first.getTypeId()).isEqualTo(20);
		assertThat(first.getTypeName()).isEqualTo("Bostadsbidrag");
		assertThat(first.getApplicantProcessAmount()).isEqualByComparingTo(new BigDecimal("1850"));
		assertThat(first.getApplicantAmountDate()).isEqualTo(date);
	}

	@Test
	void incomeRowsSumsSameTypeSameRecipientLines() {
		// the application/nyansökan path emits one line per raw declared income with no folding: two OTHER_INCOME for the
		// same applicant resolve to the same FamilyCare type id + recipient and must be summed, not collapsed to the first.
		final var firstDate = OffsetDateTime.parse("2026-05-15T00:00:00Z");
		final var secondDate = OffsetDateTime.parse("2026-05-20T00:00:00Z");
		final var lines = List.of(
			new FamilyCareIncomeLine(40, "Övriga inkomster", "APPLICANT", new BigDecimal("1500"), firstDate, "first"),
			new FamilyCareIncomeLine(40, "Övriga inkomster", "APPLICANT", new BigDecimal("2500"), secondDate, "second"),
			new FamilyCareIncomeLine(40, "Övriga inkomster", "CO_APPLICANT", new BigDecimal("800"), firstDate, null));

		final var rows = feeder.incomeRows(ERRAND_ID, lines);

		assertThat(rows).hasSize(1);
		final var row = rows.getFirst();
		assertThat(row.getTypeId()).isEqualTo(40);
		assertThat(row.getTypeName()).isEqualTo("Övriga inkomster");
		// 1500 + 2500 summed, not just the first line's 1500
		assertThat(row.getApplicantProcessAmount()).isEqualByComparingTo(new BigDecimal("4000"));
		// non-amount fields come from the first line in the recipient group
		assertThat(row.getApplicantAmountDate()).isEqualTo(firstDate);
		assertThat(row.getNote()).isEqualTo("first");
		assertThat(row.getCoapplicantProcessAmount()).isEqualByComparingTo(new BigDecimal("800"));
		assertThat(row.getCoapplicantAmountDate()).isEqualTo(firstDate);
	}

	@Test
	void incomeRowsLeavesAbsentRecipientAmountNull() {
		// only an applicant line for the type → the co-applicant side stays null (not zero)
		final var lines = List.of(
			new FamilyCareIncomeLine(40, "Övriga inkomster", "APPLICANT", new BigDecimal("1500"), null, null));

		final var rows = feeder.incomeRows(ERRAND_ID, lines);

		assertThat(rows).hasSize(1);
		assertThat(rows.getFirst().getApplicantProcessAmount()).isEqualByComparingTo(new BigDecimal("1500"));
		assertThat(rows.getFirst().getCoapplicantProcessAmount()).isNull();
	}

	@Test
	void incomeRowsHandlesNullLines() {
		assertThat(feeder.incomeRows(ERRAND_ID, null)).isEmpty();
	}

	@Test
	void expenseFeedCapsEachCostAndRaisesCapWarning() {
		final var rent = FaCost.create().withCostType("RENT").withOtherSubType(null).withSpecification("spec")
			.withAppliedAmount(new BigDecimal("9000"));
		final var errand = FinancialAssistanceEntity.create()
			.withHousingForm("RENTAL").withHousingPersonCount(2).withNormType(List.of("NATIONAL_NORM"))
			.withCosts(List.of(rent));

		when(expenseRulesServiceMock.verdict(eq(MUNICIPALITY_ID), eq("RENT"), any(), any(), any(), any(), any()))
			.thenReturn(new ExpenseRulesService.ExpenseVerdict(new BigDecimal("8500"), "SPECIAL_EXPENSE", false, null));

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand, Map.of(), null);

		assertThat(feed.rows()).hasSize(1);
		final var row = feed.rows().getFirst();
		assertThat(row.getErrandId()).isEqualTo(ERRAND_ID);
		assertThat(row.getOrigin()).isEqualTo(ORIGIN_SYSTEM);
		assertThat(row.getCostType()).isEqualTo("RENT");
		assertThat(row.getSpecification()).isEqualTo("spec");
		assertThat(row.getAppliedAmount()).isEqualByComparingTo(new BigDecimal("9000"));
		assertThat(row.getProcessAmount()).isEqualByComparingTo(new BigDecimal("8500"));
		assertThat(row.getBucket()).isEqualTo("SPECIAL_EXPENSE");

		// 8500 < 9000 → a cap warning, no review flag
		assertThat(feed.warnings()).extracting(WarningService.WarningInput::type).containsExactly(WarningService.TYPE_EXPENSE_CAPPED);
		final var warning = feed.warnings().getFirst();
		assertThat(warning.sourceKey()).isEqualTo("RENT");
		assertThat(warning.message()).contains("Kapad kostnad: Hyra").contains("9000").contains("8500");
	}

	@Test
	void expenseFeedRaisesReviewWarningWhenFlagged() {
		final var other = FaCost.create().withCostType("OTHER").withOtherSubType("BEGRAVNING").withAppliedAmount(new BigDecimal("5000"));
		final var errand = FinancialAssistanceEntity.create().withCosts(List.of(other));

		when(expenseRulesServiceMock.verdict(eq(MUNICIPALITY_ID), eq("OTHER"), any(), any(), any(), any(), any()))
			.thenReturn(new ExpenseRulesService.ExpenseVerdict(new BigDecimal("5000"), "SPECIAL_EXPENSE", true, "Övrigt bistånd – skälighet bedöms manuellt"));

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand, Map.of(), null);

		// applied == process (no cap), but flagged → a single review warning, sourceKey carries the sub-type
		assertThat(feed.warnings()).extracting(WarningService.WarningInput::type).containsExactly(WarningService.TYPE_EXPENSE_REVIEW);
		final var warning = feed.warnings().getFirst();
		assertThat(warning.sourceKey()).isEqualTo("OTHER:BEGRAVNING");
		assertThat(warning.message()).isEqualTo("Övrigt bistånd (BEGRAVNING): Övrigt bistånd – skälighet bedöms manuellt");
	}

	@Test
	void expenseFeedFlaggedAndCappedRaisesBothWarnings() {
		final var cost = FaCost.create().withCostType("RENT").withAppliedAmount(new BigDecimal("9000"));
		final var errand = FinancialAssistanceEntity.create().withCosts(List.of(cost));

		when(expenseRulesServiceMock.verdict(eq(MUNICIPALITY_ID), eq("RENT"), any(), any(), any(), any(), any()))
			.thenReturn(new ExpenseRulesService.ExpenseVerdict(new BigDecimal("7500"), "EXPENSE", true, "Rent över schablon"));

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand, Map.of(), null);

		assertThat(feed.warnings()).extracting(WarningService.WarningInput::type)
			.containsExactly(WarningService.TYPE_EXPENSE_REVIEW, WarningService.TYPE_EXPENSE_CAPPED);
	}

	@Test
	void expenseFeedReviewWarningUsesDefaultMessageWhenRegelMissing() {
		final var cost = FaCost.create().withCostType("MEDICINE").withAppliedAmount(new BigDecimal("800"));
		final var errand = FinancialAssistanceEntity.create().withCosts(List.of(cost));

		when(expenseRulesServiceMock.verdict(eq(MUNICIPALITY_ID), eq("MEDICINE"), any(), any(), any(), any(), any()))
			.thenReturn(new ExpenseRulesService.ExpenseVerdict(new BigDecimal("800"), "SPECIAL_EXPENSE", true, null));

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand, Map.of(), null);

		assertThat(feed.warnings()).extracting(WarningService.WarningInput::type).containsExactly(WarningService.TYPE_EXPENSE_REVIEW);
		assertThat(feed.warnings().getFirst().message()).isEqualTo("Medicin: Utgiften kräver en manuell skälighetsbedömning");
	}

	@Test
	void expenseFeedToleratesNullAppliedAmount() {
		final var cost = FaCost.create().withCostType("OTHER").withAppliedAmount(null);
		final var errand = FinancialAssistanceEntity.create().withCosts(List.of(cost));

		when(expenseRulesServiceMock.verdict(eq(MUNICIPALITY_ID), eq("OTHER"), any(), any(), any(), any(), any()))
			.thenReturn(new ExpenseRulesService.ExpenseVerdict(null, "EXPENSE", false, null));

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand, Map.of(), null);

		// null applied → no cap; not flagged → no warning, but the row is still produced
		assertThat(feed.rows()).hasSize(1);
		assertThat(feed.warnings()).isEmpty();
	}

	@Test
	void expenseFeedFallbackVerdictRaisesNoWarning() {
		final var cost = FaCost.create().withCostType("ELECTRICITY").withAppliedAmount(new BigDecimal("1200"));
		final var errand = FinancialAssistanceEntity.create().withCosts(List.of(cost));

		when(expenseRulesServiceMock.verdict(eq(MUNICIPALITY_ID), eq("ELECTRICITY"), any(), any(), any(), any(), any()))
			.thenReturn(new ExpenseRulesService.ExpenseVerdict(new BigDecimal("1200"), "EXPENSE", false, null));

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand, Map.of(), null);

		assertThat(feed.rows()).hasSize(1);
		assertThat(feed.warnings()).isEmpty();
	}

	@Test
	void expenseFeedHandlesNullCosts() {
		final var errand = FinancialAssistanceEntity.create();

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand, Map.of(), null);

		assertThat(feed.rows()).isEmpty();
		assertThat(feed.warnings()).isEmpty();
	}

	@Test
	void expenseFeedThreadsHistoryAgeChildrenAndHouseholdSizeIntoTheVerdict() {
		final var rent = FaCost.create().withCostType("RENT").withAppliedAmount(new BigDecimal("9000"));
		final var errand = FinancialAssistanceEntity.create()
			.withHousingPersonCount(3)
			.withChildren(List.of(FaChild.create().withPartyId("c-1"), FaChild.create().withPartyId("c-2")))
			.withCosts(List.of(rent));

		when(expenseRulesServiceMock.verdict(any(), any(), any(), any(), any(), any(), any()))
			.thenReturn(new ExpenseRulesService.ExpenseVerdict(new BigDecimal("9000"), "EXPENSE", false, null));

		feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand, Map.of("RENT", BigDecimal.valueOf(7000.0)), 35);

		final var applied = ArgumentCaptor.forClass(BigDecimal.class);
		final var previous = ArgumentCaptor.forClass(BigDecimal.class);
		final var age = ArgumentCaptor.forClass(Integer.class);
		final var children = ArgumentCaptor.forClass(Integer.class);
		final var household = ArgumentCaptor.forClass(Integer.class);
		verify(expenseRulesServiceMock).verdict(eq(MUNICIPALITY_ID), eq("RENT"), applied.capture(),
			previous.capture(), age.capture(), children.capture(), household.capture());

		assertThat(applied.getValue()).isEqualByComparingTo(new BigDecimal("9000"));
		assertThat(previous.getValue()).isEqualByComparingTo(new BigDecimal("7000"));
		assertThat(age.getValue()).isEqualTo(35);
		assertThat(children.getValue()).isEqualTo(2);
		assertThat(household.getValue()).isEqualTo(3);
	}

	@Test
	void expenseFeedPassesNullHistoryWhenCostTypeAbsentFromPreviousAmounts() {
		final var rent = FaCost.create().withCostType("RENT").withAppliedAmount(new BigDecimal("9000"));
		final var errand = FinancialAssistanceEntity.create().withCosts(List.of(rent));

		when(expenseRulesServiceMock.verdict(any(), any(), any(), any(), any(), any(), any()))
			.thenReturn(new ExpenseRulesService.ExpenseVerdict(BigDecimal.ZERO, "EXPENSE", true, "historik saknas"));

		// only ELECTRICITY has history → RENT's godkandForra is null, and household falls back to persons + children (0)
		feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand, Map.of("ELECTRICITY", BigDecimal.valueOf(1000.0)), 40);

		final var previous = ArgumentCaptor.forClass(BigDecimal.class);
		final var household = ArgumentCaptor.forClass(Integer.class);
		verify(expenseRulesServiceMock).verdict(any(), eq("RENT"), any(), previous.capture(), any(), any(), household.capture());
		assertThat(previous.getValue()).isNull();
		assertThat(household.getValue()).isZero();
	}

	@Test
	void applicationExpenseRowsUseAppliedAmountAndStaticBucketWithoutRulesOrWarnings() {
		final var rent = FaCost.create().withCostType("RENT").withSpecification("spec").withAppliedAmount(new BigDecimal("9000"));
		final var medicine = FaCost.create().withCostType("MEDICINE").withAppliedAmount(new BigDecimal("400"));
		final var errand = FinancialAssistanceEntity.create().withCosts(List.of(rent, medicine));

		final var rows = feeder.applicationExpenseRows(ERRAND_ID, errand);

		assertThat(rows).hasSize(2);
		final var rentRow = rows.getFirst();
		assertThat(rentRow.getCostType()).isEqualTo("RENT");
		assertThat(rentRow.getProcessAmount()).isEqualByComparingTo(new BigDecimal("9000"));
		assertThat(rentRow.getAppliedAmount()).isEqualByComparingTo(new BigDecimal("9000"));
		assertThat(rentRow.getBucket()).isEqualTo("EXPENSE");
		assertThat(rows.get(1).getBucket()).isEqualTo("SPECIAL_EXPENSE");
		verifyNoInteractions(expenseRulesServiceMock);
	}

	@Test
	void applicationExpenseRowsHandlesNullCosts() {
		assertThat(feeder.applicationExpenseRows(ERRAND_ID, FinancialAssistanceEntity.create())).isEmpty();
	}

	@Test
	void personRowsMapsPersonsAndChildren() {
		final var applicant = FaPerson.create().withRole("APPLICANT").withPartyId("p-1");
		final var child = FaChild.create().withPartyId("c-1").withFirstName("Anna").withLastName("Svensson").withDaysInHome(15);
		final var childNoDays = FaChild.create().withPartyId("c-2").withFirstName("Bo").withLastName(null).withDaysInHome(null);
		final var errand = FinancialAssistanceEntity.create()
			.withPersons(List.of(applicant))
			.withChildren(List.of(child, childNoDays));

		final var rows = feeder.personRows(ERRAND_ID, errand);

		assertThat(rows).hasSize(3)
			.allMatch(r -> ERRAND_ID.equals(r.getErrandId()) && ORIGIN_SYSTEM.equals(r.getOrigin()));

		final var personRow = rows.getFirst();
		assertThat(personRow.getPartyId()).isEqualTo("p-1");
		assertThat(personRow.getRole()).isEqualTo("APPLICANT");
		assertThat(personRow.getProcessDays()).isEqualTo(30);

		final var childRow = rows.get(1);
		assertThat(childRow.getPartyId()).isEqualTo("c-1");
		assertThat(childRow.getRole()).isEqualTo(ROLE_CHILD);
		assertThat(childRow.getName()).isEqualTo("Anna Svensson");
		assertThat(childRow.getProcessDays()).isEqualTo(15);

		final var childNoDaysRow = rows.get(2);
		assertThat(childNoDaysRow.getName()).isEqualTo("Bo");
		assertThat(childNoDaysRow.getProcessDays()).isEqualTo(30);
	}

	@Test
	void personRowsHandlesNullCollections() {
		assertThat(feeder.personRows(ERRAND_ID, FinancialAssistanceEntity.create())).isEmpty();
	}

	@Test
	void householdDeltaWarningsReturnsEmptyForEmptyPrevious() {
		final var current = List.of(FaNormPersonEntity.create().withPartyId("p-1"));
		final var errand = FinancialAssistanceEntity.create();

		assertThat(feeder.householdDeltaWarnings(MUNICIPALITY_ID, errand, current, PreviousHousehold.empty())).isEmpty();
		assertThat(feeder.householdDeltaWarnings(MUNICIPALITY_ID, errand, current, null)).isEmpty();
	}

	@Test
	void householdDeltaWarningsFlagsSizeChangeWhenDmnFlags() {
		final var current = List.of(FaNormPersonEntity.create().withPartyId("p-1"));
		final var previous = new PreviousHousehold(Set.of("p-1", "p-2"), 2, null, null);
		final var errand = FinancialAssistanceEntity.create();

		when(renewalDeltaServiceMock.classify(eq(MUNICIPALITY_ID), eq("HOUSEHOLD_SIZE"), eq(-1), any()))
			.thenReturn(new RenewalDeltaService.DeltaVerdict(true, "Kontrollera hushållets sammansättning"));

		final var warnings = feeder.householdDeltaWarnings(MUNICIPALITY_ID, errand, current, previous);

		assertThat(warnings).extracting(WarningService.WarningInput::type).containsExactly(WarningService.TYPE_HOUSEHOLD_CHANGE);
		final var warning = warnings.getFirst();
		assertThat(warning.sourceKey()).isEqualTo("household-size");
		assertThat(warning.message())
			.contains("tidigare 2, nu 1")
			.contains("saknas nu: p-2")
			.contains("Kontrollera hushållets sammansättning");
	}

	@Test
	void householdDeltaWarningsSkipsSizeChangeWhenDmnDoesNotFlag() {
		final var current = List.of(
			FaNormPersonEntity.create().withPartyId("p-1"),
			FaNormPersonEntity.create().withPartyId("p-2"));
		final var previous = new PreviousHousehold(Set.of("p-1"), 1, null, null);
		final var errand = FinancialAssistanceEntity.create();

		when(renewalDeltaServiceMock.classify(eq(MUNICIPALITY_ID), eq("HOUSEHOLD_SIZE"), eq(1), any()))
			.thenReturn(new RenewalDeltaService.DeltaVerdict(false, "Oförändrat"));

		assertThat(feeder.householdDeltaWarnings(MUNICIPALITY_ID, errand, current, previous)).isEmpty();
	}

	@Test
	void householdDeltaWarningsFlagsHousingCostChange() {
		final var current = List.of(FaNormPersonEntity.create().withPartyId("p-1"));
		final var previous = new PreviousHousehold(Set.of("p-1"), 1, null, BigDecimal.valueOf(5000.0));
		final var errand = FinancialAssistanceEntity.create()
			.withCosts(List.of(FaCost.create().withCostType("RENT").withAppliedAmount(new BigDecimal("6600"))));

		// same household → only the housing delta is consulted; (6600-5000)/5000 = +32%
		when(renewalDeltaServiceMock.classify(MUNICIPALITY_ID, "HOUSING_COST", 0, new BigDecimal("32")))
			.thenReturn(new RenewalDeltaService.DeltaVerdict(true, "Väsentlig ökning – kontrollera hyresunderlag"));

		final var warnings = feeder.householdDeltaWarnings(MUNICIPALITY_ID, errand, current, previous);

		assertThat(warnings).extracting(WarningService.WarningInput::type).containsExactly(WarningService.TYPE_HOUSING_COST_CHANGE);
		final var warning = warnings.getFirst();
		assertThat(warning.sourceKey()).isEqualTo("housing-cost");
		assertThat(warning.message())
			.contains("Boendekostnaden har ändrats +32%")
			.contains("tidigare 5000 kr → nu 6600 kr")
			.contains("Väsentlig ökning");
	}

	@Test
	void householdDeltaWarningsFlagsBothSizeAndHousing() {
		final var current = List.of(FaNormPersonEntity.create().withPartyId("p-1"));
		final var previous = new PreviousHousehold(Set.of("p-1", "p-2"), 2, null, BigDecimal.valueOf(5000.0));
		final var errand = FinancialAssistanceEntity.create()
			.withCosts(List.of(FaCost.create().withCostType("RENT").withAppliedAmount(new BigDecimal("2500"))));

		when(renewalDeltaServiceMock.classify(eq(MUNICIPALITY_ID), eq("HOUSEHOLD_SIZE"), eq(-1), any()))
			.thenReturn(new RenewalDeltaService.DeltaVerdict(true, "Kontrollera"));
		when(renewalDeltaServiceMock.classify(MUNICIPALITY_ID, "HOUSING_COST", 0, new BigDecimal("-50")))
			.thenReturn(new RenewalDeltaService.DeltaVerdict(true, "Väsentlig minskning"));

		final var warnings = feeder.householdDeltaWarnings(MUNICIPALITY_ID, errand, current, previous);

		assertThat(warnings).extracting(WarningService.WarningInput::type)
			.containsExactly(WarningService.TYPE_HOUSEHOLD_CHANGE, WarningService.TYPE_HOUSING_COST_CHANGE);
	}

	@Test
	void householdDeltaWarningsSkipsHousingWhenNoPreviousCost() {
		final var current = List.of(FaNormPersonEntity.create().withPartyId("p-1"));
		final var previous = new PreviousHousehold(Set.of("p-1"), 1, null, null);
		final var errand = FinancialAssistanceEntity.create()
			.withCosts(List.of(FaCost.create().withCostType("RENT").withAppliedAmount(new BigDecimal("6000"))));

		// no size change and no previous housing cost → the delta DMN is never consulted
		assertThat(feeder.householdDeltaWarnings(MUNICIPALITY_ID, errand, current, previous)).isEmpty();
	}
}
