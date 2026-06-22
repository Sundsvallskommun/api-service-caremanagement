package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.service.model.FcIncomeLine;
import se.sundsvall.caremanagement.lifecare.service.model.PreviousHousehold;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaChild;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaCost;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaNormPersonEntity;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaPerson;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FinancialAssistanceEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ORIGIN_SYSTEM;
import static se.sundsvall.caremanagement.types.financialassistance.service.NormberakningConstants.ROLE_CHILD;

@ExtendWith(MockitoExtension.class)
class NormberakningFeederTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String ERRAND_ID = "errand-1";

	@Mock
	private ExpenseRegelverkService expenseRegelverkServiceMock;

	@Mock
	private AteransokanDeltaService ateransokanDeltaServiceMock;

	@InjectMocks
	private NormberakningFeeder feeder;

	@Test
	void incomeRowsMapsEachLine() {
		final var date = OffsetDateTime.parse("2026-05-15T00:00:00Z");
		final var lines = List.of(
			new FcIncomeLine(20, "Bostadsbidrag", "APPLICANT", new BigDecimal("1850"), date, "note"),
			new FcIncomeLine(21, "Lön", "CO_APPLICANT", new BigDecimal("12000"), date, null));

		final var rows = feeder.incomeRows(ERRAND_ID, lines);

		assertThat(rows).hasSize(2);
		assertThat(rows).allMatch(r -> ERRAND_ID.equals(r.getErrandId()) && ORIGIN_SYSTEM.equals(r.getOrigin()));
		final var first = rows.getFirst();
		assertThat(first.getTypeId()).isEqualTo(20);
		assertThat(first.getTypeName()).isEqualTo("Bostadsbidrag");
		assertThat(first.getApplicantProcessAmount()).isEqualByComparingTo(new BigDecimal("1850"));
		assertThat(first.getApplicantAmountDate()).isEqualTo(date);
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
			.withHousingForm("RENTAL").withHousingPersonCount(2).withNormType("RIKSNORM")
			.withCosts(List.of(rent));

		when(expenseRegelverkServiceMock.verdict(eq(MUNICIPALITY_ID), eq("RENT"), any(), eq("RENTAL"), eq(2), eq("RIKSNORM"), eq(new BigDecimal("9000"))))
			.thenReturn(new ExpenseRegelverkService.ExpenseVerdict(new BigDecimal("8500"), "SPECIAL_EXPENSE", false, null));

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand);

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
		assertThat(warning.message()).contains("Kapad kostnad: RENT").contains("9000").contains("8500");
	}

	@Test
	void expenseFeedRaisesReviewWarningWhenFlagged() {
		final var other = FaCost.create().withCostType("OTHER").withOtherSubType("BEGRAVNING").withAppliedAmount(new BigDecimal("5000"));
		final var errand = FinancialAssistanceEntity.create().withCosts(List.of(other));

		when(expenseRegelverkServiceMock.verdict(eq(MUNICIPALITY_ID), eq("OTHER"), eq("BEGRAVNING"), any(), any(), any(), eq(new BigDecimal("5000"))))
			.thenReturn(new ExpenseRegelverkService.ExpenseVerdict(new BigDecimal("5000"), "SPECIAL_EXPENSE", true, "Övrigt bistånd – skälighet bedöms manuellt"));

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand);

		// applied == process (no cap), but flagged → a single review warning, sourceKey carries the sub-type
		assertThat(feed.warnings()).extracting(WarningService.WarningInput::type).containsExactly(WarningService.TYPE_EXPENSE_REVIEW);
		final var warning = feed.warnings().getFirst();
		assertThat(warning.sourceKey()).isEqualTo("OTHER:BEGRAVNING");
		assertThat(warning.message()).isEqualTo("OTHER (BEGRAVNING): Övrigt bistånd – skälighet bedöms manuellt");
	}

	@Test
	void expenseFeedFlaggedAndCappedRaisesBothWarnings() {
		final var cost = FaCost.create().withCostType("RENT").withAppliedAmount(new BigDecimal("9000"));
		final var errand = FinancialAssistanceEntity.create().withCosts(List.of(cost));

		when(expenseRegelverkServiceMock.verdict(eq(MUNICIPALITY_ID), eq("RENT"), any(), any(), any(), any(), eq(new BigDecimal("9000"))))
			.thenReturn(new ExpenseRegelverkService.ExpenseVerdict(new BigDecimal("7500"), "EXPENSE", true, "Hyra över schablon"));

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand);

		assertThat(feed.warnings()).extracting(WarningService.WarningInput::type)
			.containsExactly(WarningService.TYPE_EXPENSE_REVIEW, WarningService.TYPE_EXPENSE_CAPPED);
	}

	@Test
	void expenseFeedReviewWarningUsesDefaultMessageWhenRegelMissing() {
		final var cost = FaCost.create().withCostType("MEDICINE").withAppliedAmount(new BigDecimal("800"));
		final var errand = FinancialAssistanceEntity.create().withCosts(List.of(cost));

		when(expenseRegelverkServiceMock.verdict(eq(MUNICIPALITY_ID), eq("MEDICINE"), any(), any(), any(), any(), eq(new BigDecimal("800"))))
			.thenReturn(new ExpenseRegelverkService.ExpenseVerdict(new BigDecimal("800"), "EXPENSE", true, null));

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand);

		assertThat(feed.warnings()).extracting(WarningService.WarningInput::type).containsExactly(WarningService.TYPE_EXPENSE_REVIEW);
		assertThat(feed.warnings().getFirst().message()).isEqualTo("MEDICINE: Utgiften kräver manuell skälighetsbedömning");
	}

	@Test
	void expenseFeedToleratesNullAppliedAmount() {
		final var cost = FaCost.create().withCostType("OTHER").withAppliedAmount(null);
		final var errand = FinancialAssistanceEntity.create().withCosts(List.of(cost));

		when(expenseRegelverkServiceMock.verdict(eq(MUNICIPALITY_ID), eq("OTHER"), any(), any(), any(), any(), any()))
			.thenReturn(new ExpenseRegelverkService.ExpenseVerdict(null, "EXPENSE", false, null));

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand);

		// null applied → no cap; not flagged → no warning, but the row is still produced
		assertThat(feed.rows()).hasSize(1);
		assertThat(feed.warnings()).isEmpty();
	}

	@Test
	void expenseFeedFallbackVerdictRaisesNoWarning() {
		final var cost = FaCost.create().withCostType("ELECTRICITY").withAppliedAmount(new BigDecimal("1200"));
		final var errand = FinancialAssistanceEntity.create().withCosts(List.of(cost));

		when(expenseRegelverkServiceMock.verdict(eq(MUNICIPALITY_ID), eq("ELECTRICITY"), any(), any(), any(), any(), eq(new BigDecimal("1200"))))
			.thenReturn(new ExpenseRegelverkService.ExpenseVerdict(new BigDecimal("1200"), "EXPENSE", false, null));

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand);

		assertThat(feed.rows()).hasSize(1);
		assertThat(feed.warnings()).isEmpty();
	}

	@Test
	void expenseFeedHandlesNullCosts() {
		final var errand = FinancialAssistanceEntity.create();

		final var feed = feeder.expenseFeed(MUNICIPALITY_ID, ERRAND_ID, errand);

		assertThat(feed.rows()).isEmpty();
		assertThat(feed.warnings()).isEmpty();
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

		assertThat(rows).hasSize(3);
		assertThat(rows).allMatch(r -> ERRAND_ID.equals(r.getErrandId()) && ORIGIN_SYSTEM.equals(r.getOrigin()));

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

		when(ateransokanDeltaServiceMock.classify(eq(MUNICIPALITY_ID), eq("HOUSEHOLD_SIZE"), eq(-1), any()))
			.thenReturn(new AteransokanDeltaService.DeltaVerdict(true, "Kontrollera hushållets sammansättning"));

		final var warnings = feeder.householdDeltaWarnings(MUNICIPALITY_ID, errand, current, previous);

		assertThat(warnings).extracting(WarningService.WarningInput::type).containsExactly(WarningService.TYPE_HOUSEHOLD_CHANGE);
		final var warning = warnings.getFirst();
		assertThat(warning.sourceKey()).isEqualTo("hushall-storlek");
		assertThat(warning.message())
			.contains("föregående 2, nu 1")
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

		when(ateransokanDeltaServiceMock.classify(eq(MUNICIPALITY_ID), eq("HOUSEHOLD_SIZE"), eq(1), any()))
			.thenReturn(new AteransokanDeltaService.DeltaVerdict(false, "Oförändrat"));

		assertThat(feeder.householdDeltaWarnings(MUNICIPALITY_ID, errand, current, previous)).isEmpty();
	}

	@Test
	void householdDeltaWarningsFlagsBoendeCostChange() {
		final var current = List.of(FaNormPersonEntity.create().withPartyId("p-1"));
		final var previous = new PreviousHousehold(Set.of("p-1"), 1, null, 5000.0);
		final var errand = FinancialAssistanceEntity.create()
			.withCosts(List.of(FaCost.create().withCostType("RENT").withAppliedAmount(new BigDecimal("6600"))));

		// same household → only the boende delta is consulted; (6600-5000)/5000 = +32%
		when(ateransokanDeltaServiceMock.classify(eq(MUNICIPALITY_ID), eq("HOUSING_COST"), eq(0), eq(new BigDecimal("32"))))
			.thenReturn(new AteransokanDeltaService.DeltaVerdict(true, "Väsentlig ökning – kontrollera hyresunderlag"));

		final var warnings = feeder.householdDeltaWarnings(MUNICIPALITY_ID, errand, current, previous);

		assertThat(warnings).extracting(WarningService.WarningInput::type).containsExactly(WarningService.TYPE_HOUSING_COST_CHANGE);
		final var warning = warnings.getFirst();
		assertThat(warning.sourceKey()).isEqualTo("boende-kostnad");
		assertThat(warning.message())
			.contains("Boendekostnad ändrad +32%")
			.contains("föregående 5000 kr → nu 6600 kr")
			.contains("Väsentlig ökning");
	}

	@Test
	void householdDeltaWarningsFlagsBothSizeAndBoende() {
		final var current = List.of(FaNormPersonEntity.create().withPartyId("p-1"));
		final var previous = new PreviousHousehold(Set.of("p-1", "p-2"), 2, null, 5000.0);
		final var errand = FinancialAssistanceEntity.create()
			.withCosts(List.of(FaCost.create().withCostType("RENT").withAppliedAmount(new BigDecimal("2500"))));

		when(ateransokanDeltaServiceMock.classify(eq(MUNICIPALITY_ID), eq("HOUSEHOLD_SIZE"), eq(-1), any()))
			.thenReturn(new AteransokanDeltaService.DeltaVerdict(true, "Kontrollera"));
		when(ateransokanDeltaServiceMock.classify(eq(MUNICIPALITY_ID), eq("HOUSING_COST"), eq(0), eq(new BigDecimal("-50"))))
			.thenReturn(new AteransokanDeltaService.DeltaVerdict(true, "Väsentlig minskning"));

		final var warnings = feeder.householdDeltaWarnings(MUNICIPALITY_ID, errand, current, previous);

		assertThat(warnings).extracting(WarningService.WarningInput::type)
			.containsExactly(WarningService.TYPE_HOUSEHOLD_CHANGE, WarningService.TYPE_HOUSING_COST_CHANGE);
	}

	@Test
	void householdDeltaWarningsSkipsBoendeWhenNoPreviousCost() {
		final var current = List.of(FaNormPersonEntity.create().withPartyId("p-1"));
		final var previous = new PreviousHousehold(Set.of("p-1"), 1, null, null);
		final var errand = FinancialAssistanceEntity.create()
			.withCosts(List.of(FaCost.create().withCostType("RENT").withAppliedAmount(new BigDecimal("6000"))));

		// no size change and no previous boende cost → the delta DMN is never consulted
		assertThat(feeder.householdDeltaWarnings(MUNICIPALITY_ID, errand, current, previous)).isEmpty();
	}
}
