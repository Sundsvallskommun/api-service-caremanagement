package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormHeaderInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningRowInput;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.objects;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.addExpense;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.addIncome;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.changeExpense;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.changeHeader;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.changeIncome;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.changePerson;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.householdSizeChanged;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.lifecareDay;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.needsRecount;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.removeExpense;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.removeIncome;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.sharedCostShare;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationRowChanges.withEnteredIncomes;

class CalculationRowChangesTest {

	private static final String INCOME = """
		{"calculationId":30,"serialNumber":10000,"incomeType":"Aktivitetsstöd","amountApplicant":5600,"applicantSearchDate":"2026-06-17","applicantNote":"",
		 "amountCoApplicant":0,"coApplicantSearchDate":"","grossAmountApplicant":5600,"grossAmountCoApplicant":0,"incomeCode":19}""";

	private static final String EXPENSE = """
		{"calculationId":30,"serialNumber":10000,"expenseCode":3,"expenseType":"Boendekostnad","appliedAmount":%d,"approvedAmount":%d,"note":""}""";

	private static ObjectNode calculation(final String incomes, final String expenses) {
		return json("""
			{"calculationId":30,"normId":1,"normText":"Riksnorm 2026","date":"2026-09-24","startDate":"2026-09-01","endDate":"2026-09-30",
			 "calculationPersons":[{"personId":"19880209T050","personKey":1,"name":"Testsson, Test","normRowId":2,"normRow":"Ensamstående","amount":3940,"included":true,
			   "deviationFromDate":"2026-09-05","deviationToDate":"","personIdFormatted":"880209-T050"},
			  {"personId":"20141201T010","personKey":2,"name":"Testbarn Test, Testar","normRow":null,"amount":0,"included":true,"deviationFromDate":"","deviationToDate":"",
			   "personIdFormatted":"141201-T010"},
			  {"personId":"20150101T020","name":"Ny","included":false}],
			 "norm":{"rows":[{"rowId":12,"name":"Barn 11-14 4390.00","monthlyAmount":4390,"dailyAmount":144},{"rowId":2,"name":"Ensamstående 3940.00"}],
			   "shared":[{"normId":1,"noOfMembers":2,"monthlyAmount":1500},{"normId":1,"noOfMembers":4,"monthlyAmount":2030}]},
			 "calculationIncomes":[%s],"calculationExpenses":[%s],"calculationSpecialExpenses":[],"hasCustomHouseholdSize":false,"isFinalized":false}"""
			.formatted(incomes, expenses));
	}

	private static ObjectNode calculation() {
		return calculation(INCOME, EXPENSE.formatted(5000, 5000));
	}

	private static ObjectNode forEdit() {
		return json("""
			{"norms":[{"normId":1,"name":"Riksnorm 2026"},{"normId":6,"name":"Specnorm"}],
			 "incomeTypes":[{"id":19,"text":"Aktivitetsstöd","isActive":true},{"id":1,"text":"Lön efter skatt","isActive":true,"isJobStimulus":true,"jobStimulusPercent":25}],
			 "expenseTypes":[{"id":3,"text":"Boendekostnad","isActive":true},{"id":8,"text":"A-kasseavgift","isActive":true}],
			 "specialExpenseTypes":[{"id":3,"text":"Glasögon","isActive":true}]}""");
	}

	@Test
	void addsAnIncomeOfALifecareTypeByName() {
		final var changed = addIncome(calculation(), forEdit().path("incomeTypes"),
			NormberakningRowInput.create().withTypeName("Lön efter skatt").withApplicantCaseworkerAmount(BigDecimal.valueOf(5000)));

		final var added = objects(changed, "calculationIncomes").getLast();
		assertThat(added).isEqualTo(json("""
			{"calculationId":30,"serialNumber":0,"incomeType":"Lön efter skatt","amountApplicant":5000,"applicantSearchDate":"","applicantNote":null,
			 "amountCoApplicant":0,"coApplicantSearchDate":"","grossAmountApplicant":5000,"grossAmountCoApplicant":0,"incomeCode":1,"changeable":true,"isValid":true}"""));
	}

	@Test
	void addsAnIncomeByCodeReplacingARowLifecareWouldDrop() {
		final var changed = addIncome(calculation(INCOME.replace("5600", "0"), EXPENSE.formatted(1, 1)), forEdit().path("incomeTypes"),
			NormberakningRowInput.create().withTypeId(19).withApplicantCaseworkerAmount(BigDecimal.ONE));

		assertThat(objects(changed, "calculationIncomes")).hasSize(1);
		assertThat(objects(changed, "calculationIncomes").getFirst().path("serialNumber").intValue()).isZero();
	}

	@Test
	void refusesAnIncomeTypeAlreadyThereOrUnknown() {
		assertThatThrownBy(() -> addIncome(calculation(), forEdit().path("incomeTypes"), NormberakningRowInput.create().withTypeName("Aktivitetsstöd")))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("Aktivitetsstöd finns redan");
		assertThatThrownBy(() -> addIncome(calculation(), forEdit().path("incomeTypes"), NormberakningRowInput.create().withTypeName("Okänd")))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("ingen inkomsttyp \"Okänd\"");
		assertThatThrownBy(() -> addIncome(calculation(), forEdit().path("incomeTypes"), NormberakningRowInput.create().withTypeId(77)))
			.hasMessageContaining("\"77\"");
	}

	@Test
	void changesAnIncomeByItsCodeDateAndNoteIncluded() {
		final var changed = changeIncome(calculation(), "19", NormberakningRowInput.create()
			.withApplicantCaseworkerAmount(BigDecimal.valueOf(6000))
			.withApplicantAmountDate("2026-09-02T00:00:00+02:00")
			.withNote("Enligt beslut"));

		final var row = objects(changed, "calculationIncomes").getFirst();
		assertThat(row.path("amountApplicant").intValue()).isEqualTo(6000);
		assertThat(row.path("grossAmountApplicant").intValue()).isEqualTo(6000);
		assertThat(row.path("applicantSearchDate").stringValue()).isEqualTo("2026-09-02");
		assertThat(row.path("applicantNote").stringValue()).isEqualTo("Enligt beslut");
		assertThat(row.path("coApplicantSearchDate").stringValue()).isEmpty();
	}

	@Test
	void removesAnIncomeByZeroingIt() {
		final var row = objects(removeIncome(calculation(), "19"), "calculationIncomes").getFirst();

		assertThat(row.path("amountApplicant").intValue()).isZero();
		assertThat(row.path("amountCoApplicant").intValue()).isZero();
		assertThat(row.path("applicantNote").isNull()).isTrue();
		assertThatThrownBy(() -> removeIncome(calculation(), "20")).hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void addsAnUtgiftOrALevnadskostnadByLifecareCodeIntoItsOwnList() {
		final var utgift = addExpense(calculation(), forEdit(), NormberakningRowInput.create().withBucket("EXPENSE").withCostType("8").withCaseworkerAmount(BigDecimal.valueOf(120)));
		final var levnadskostnad = addExpense(calculation(), forEdit(), NormberakningRowInput.create().withBucket("SPECIAL_EXPENSE").withCostType("3")
			.withAppliedAmount(BigDecimal.valueOf(900)).withCaseworkerAmount(BigDecimal.valueOf(800)));
		final var appliedOnly = addExpense(calculation(), forEdit(), NormberakningRowInput.create().withCostType("8").withAppliedAmount(BigDecimal.valueOf(70)));

		assertThat(objects(utgift, "calculationExpenses").getLast()).isEqualTo(json("""
			{"expenseCode":8,"expenseType":"A-kasseavgift","appliedAmount":120,"approvedAmount":120,"note":null,"changeable":true,"markForCopy":false,"showMarkForCopy":false}"""));
		assertThat(objects(levnadskostnad, "calculationSpecialExpenses")).singleElement()
			.satisfies(row -> {
				assertThat(row.path("expenseType").stringValue()).isEqualTo("Glasögon");
				assertThat(row.path("appliedAmount").intValue()).isEqualTo(900);
				assertThat(row.path("approvedAmount").intValue()).isEqualTo(800);
			});
		assertThat(objects(appliedOnly, "calculationExpenses").getLast().path("approvedAmount").intValue()).isEqualTo(70);
		assertThatThrownBy(() -> addExpense(calculation(), forEdit(), NormberakningRowInput.create().withCostType("99")))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("ingen kostnadstyp \"99\"");
		assertThatThrownBy(() -> addExpense(calculation(), forEdit(), NormberakningRowInput.create()))
			.hasMessageContaining("ingen kostnadstyp \"\"");
	}

	@Test
	void changesAndRemovesTheUtgiftTheIdNamesAndNoOther() {
		final var saved = calculation(INCOME, EXPENSE.formatted(5000, 5000) + "," + EXPENSE.formatted(200, 200));

		final var changed = changeExpense(saved.deepCopy(), "E-3-2", NormberakningRowInput.create().withCaseworkerAmount(BigDecimal.valueOf(150)).withNote("delvis"));
		assertThat(objects(changed, "calculationExpenses")).extracting(row -> row.path("approvedAmount").intValue()).containsExactly(5000, 150);
		assertThat(objects(changed, "calculationExpenses")).extracting(row -> row.path("appliedAmount").intValue()).containsExactly(5000, 200);
		assertThat(objects(changed, "calculationExpenses").get(1).path("note").stringValue()).isEqualTo("delvis");

		final var removed = removeExpense(saved.deepCopy(), "E-3");
		assertThat(objects(removed, "calculationExpenses")).extracting(row -> row.path("approvedAmount").intValue()).containsExactly(0, 200);
		assertThat(objects(removed, "calculationExpenses")).extracting(row -> row.path("appliedAmount").intValue()).containsExactly(0, 200);

		assertThatThrownBy(() -> changeExpense(saved, "S-3", NormberakningRowInput.create())).hasFieldOrPropertyWithValue("status", NOT_FOUND);
		assertThatThrownBy(() -> changeExpense(saved, "X-3", NormberakningRowInput.create())).hasFieldOrPropertyWithValue("status", NOT_FOUND);
		assertThatThrownBy(() -> changeExpense(saved, "E-3-3", NormberakningRowInput.create())).hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void setsAMembersDaysAndNormintervallLeavingIngårFrånTillAsLifecareHasThem() {
		final var changed = changePerson(calculation(), "2", NormberakningRowInput.create().withCaseworkerDays(10).withNormRowId(12));

		final var child = objects(changed, "calculationPersons").get(1);
		assertThat(child.path("deviationDays").intValue()).isEqualTo(10);
		assertThat(child.path("normRowId").intValue()).isEqualTo(12);
		assertThat(child.path("normRow").stringValue()).isEqualTo("Barn 11-14");
		assertThat(objects(changed, "calculationPersons").getFirst().path("deviationFromDate").stringValue()).isEqualTo("2026-09-05");
	}

	@Test
	void setsTheWholePeriodWithoutDaysAndFindsANewMemberByItsPlace() {
		final var changed = changePerson(calculation(), "new-3", NormberakningRowInput.create());

		assertThat(objects(changed, "calculationPersons").get(2).path("deviationDays").isNull()).isTrue();
		assertThatThrownBy(() -> changePerson(calculation(), "2", NormberakningRowInput.create().withNormRowId(99)))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT);
		assertThatThrownBy(() -> changePerson(calculation(), "9", NormberakningRowInput.create()))
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);
	}

	@Test
	void recountsAMemberWhoseDaysOrNormintervallChanged() {
		final var before = calculation();
		final var sameMember = objects(before.deepCopy(), "calculationPersons").getFirst();
		final var otherDays = sameMember.deepCopy().put("deviationDays", 10);
		final var otherRow = sameMember.deepCopy().put("normRowId", 12);
		final var excluded = otherDays.deepCopy().put("included", false);
		final var unplaced = otherDays.deepCopy().put("normRowId", 0);
		final var stranger = otherDays.deepCopy().put("personId", "x");

		assertThat(needsRecount(before, sameMember)).isFalse();
		assertThat(needsRecount(before, otherDays)).isTrue();
		assertThat(needsRecount(before, otherRow)).isTrue();
		assertThat(needsRecount(before, excluded)).isFalse();
		assertThat(needsRecount(before, unplaced)).isFalse();
		assertThat(needsRecount(before, stranger)).isFalse();
	}

	@Test
	void setsAnOwnHouseholdSizeSavedForComingBeräkningar() {
		final var changed = changeHeader(calculation(), forEdit(), NormHeaderInput.create().withHasCustomHouseholdSize(true).withHouseholdSize(4));

		assertThat(changed.path("hasCustomHouseholdSize").booleanValue()).isTrue();
		assertThat(changed.path("householdSize").intValue()).isEqualTo(4);
		assertThat(changed.path("saveHouseholdSize").booleanValue()).isTrue();
	}

	@Test
	void takesAnOwnHouseholdSizeOffSoTheMembersCount() {
		final var custom = calculation().put("hasCustomHouseholdSize", true).put("householdSize", 5);

		final var changed = changeHeader(custom, forEdit(), NormHeaderInput.create().withHasCustomHouseholdSize(false));

		assertThat(changed.path("hasCustomHouseholdSize").booleanValue()).isFalse();
		assertThat(changed.path("householdSize").intValue()).isEqualTo(2);
		assertThat(changed.path("saveHouseholdSize").booleanValue()).isFalse();
		// A size alone keeps the flag the beräkning has.
		assertThat(changeHeader(calculation().put("hasCustomHouseholdSize", true), forEdit(), NormHeaderInput.create().withHouseholdSize(3)).path("householdSize").intValue()).isEqualTo(3);
	}

	@Test
	void refusesAnOwnHouseholdSizeWithoutASize() {
		assertThatThrownBy(() -> changeHeader(calculation(), forEdit(), NormHeaderInput.create().withHasCustomHouseholdSize(true)))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT);
		assertThatThrownBy(() -> changeHeader(calculation(), forEdit(), NormHeaderInput.create().withHasCustomHouseholdSize(true).withHouseholdSize(0)))
			.hasMessageContaining("Ange hushållsstorleken");
	}

	@Test
	void putsTheBeräkningOnAnotherNorm() {
		final var changed = changeHeader(calculation(), forEdit(), NormHeaderInput.create().withNormId(6));

		assertThat(changed.path("normId").intValue()).isEqualTo(6);
		assertThat(changed.path("normText").stringValue()).isEqualTo("Specnorm");
		assertThat(changed.has("saveHouseholdSize")).isFalse();
		assertThat(changeHeader(calculation(), forEdit(), NormHeaderInput.create().withNormId(1)).path("normText").stringValue()).isEqualTo("Riksnorm 2026");
	}

	@Test
	void refusesThePeriodAndANormLifecareDoesNotHave() {
		assertThatThrownBy(() -> changeHeader(calculation(), forEdit(), NormHeaderInput.create().withCalculationFromDate(LocalDate.of(2026, 9, 2))))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("Perioden ändras i Lifecare");
		assertThatThrownBy(() -> changeHeader(calculation(), forEdit(), NormHeaderInput.create().withCalculationToDate(LocalDate.of(2026, 9, 2))))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT);
		assertThatThrownBy(() -> changeHeader(calculation(), forEdit(), NormHeaderInput.create().withNormType(List.of("NATIONAL_NORM"))))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT);
		assertThatThrownBy(() -> changeHeader(calculation(), forEdit(), NormHeaderInput.create().withNormId(99)))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("Normen finns inte");
	}

	@Test
	void seesWhenTheGemensammaKostnaderChange() {
		final var before = calculation();

		assertThat(householdSizeChanged(before, before.deepCopy())).isFalse();
		assertThat(householdSizeChanged(before, before.deepCopy().put("normId", 6))).isTrue();
		assertThat(householdSizeChanged(before, before.deepCopy().put("hasCustomHouseholdSize", true).put("householdSize", 4))).isTrue();
		final var fewer = before.deepCopy();
		((ObjectNode) fewer.path("calculationPersons").get(1)).put("included", false);
		assertThat(householdSizeChanged(before, fewer)).isTrue();
	}

	@Test
	void takesTheMembersShareOfTheGemensammaKostnader() {
		final var custom = calculation().put("hasCustomHouseholdSize", true).put("householdSize", 4);

		final var shared = sharedCostShare(custom).orElseThrow();

		assertThat(shared.normShared().path("monthlyAmount").intValue()).isEqualTo(2030);
		// Two members' share of a household of four: 2 030 x 2/4.
		assertThat(shared.share(2030)).isEqualTo(1015);
		assertThat(shared.share(2031)).isEqualTo(1016);
		assertThat(sharedCostShare(calculation()).orElseThrow().size()).isEqualTo(2);
		assertThat(sharedCostShare(custom.deepCopy().put("householdSize", 3))).isEmpty();
		assertThat(sharedCostShare(json("{\"calculationPersons\":[]}"))).isEmpty();
	}

	@Test
	void putsTheGrossBackOnIncomesJobbstimulansAppliesTo() {
		final var saved = calculation("""
			{"incomeCode":1,"amountApplicant":3750,"grossAmountApplicant":5000},{"incomeCode":19,"amountApplicant":800,"grossAmountApplicant":900},
			{"incomeCode":1,"amountApplicant":10,"grossAmountApplicant":0}""", EXPENSE.formatted(1, 1));

		final var entered = objects(withEnteredIncomes(saved, forEdit().path("incomeTypes")), "calculationIncomes");

		assertThat(entered).extracting(row -> row.path("amountApplicant").intValue()).containsExactly(5000, 800, 10);
	}

	@Test
	void takesTheDayOfADateOrDateTime() {
		assertThat(lifecareDay(null)).isEmpty();
		assertThat(lifecareDay("")).isEmpty();
		assertThat(lifecareDay("2026-09-02T00:00:00+02:00")).isEqualTo("2026-09-02");
		assertThat(lifecareDay("2026-09")).isEqualTo("2026-09");
	}
}
