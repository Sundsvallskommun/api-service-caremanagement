package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.TypeOption;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningNormRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningPersonRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningPreviousPerson;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningRowInput;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.NormberakningTypeOption;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.tree;

class NormberakningMapperTest {

	private static ObjectNode calculation(final String extra) {
		return json("""
			{"calculationId":30,"normId":1,"normText":"Riksnorm 2026","date":"2026-09-24","startDate":"2026-09-01","endDate":"2026-09-30",
			 "calculationPersons":[{"personId":"19880209T050","personKey":1,"name":"Testsson, Test","normRowId":2,"normRow":"Ensamstående","amount":3940,
			   "included":true,"deviationFromDate":"","deviationToDate":"","personIdFormatted":"880209-T050"}],
			 "calculationIncomes":[{"incomeCode":19,"incomeType":"Aktivitetsstöd","amountApplicant":5600,"applicantSearchDate":"2026-06-17","applicantNote":"",
			   "amountCoApplicant":0,"coApplicantSearchDate":"","grossAmountApplicant":5600}],
			 "calculationExpenses":[{"expenseCode":3,"expenseType":"Boendekostnad","appliedAmount":5000,"approvedAmount":5000,"note":""}],
			 "calculationSpecialExpenses":[],"hasCustomHouseholdSize":false,"isFinalized":false,"updateTimestamp":"2026-09-24",
			 "sumInk":5600,"sumUtg":5000,"sumSpec":0%s}""".formatted(extra));
	}

	private static ObjectNode forEdit(final ObjectNode calculation) {
		final var forEdit = json("""
			{"norms":[{"normId":1,"name":"Riksnorm 2026"}],
			 "incomeTypes":[{"id":19,"text":"Aktivitetsstöd","isActive":true},{"id":1,"text":"Lön efter skatt","isActive":true,"isJobStimulus":true,"jobStimulusPercent":25},
			   {"id":2,"text":"Gammal","isActive":false}],
			 "expenseTypes":[{"id":3,"text":"Boendekostnad","isActive":true}],
			 "specialExpenseTypes":[{"id":3,"text":"Glasögon","isActive":true}]}""");
		forEdit.set("calculation", calculation);
		return forEdit;
	}

	@Test
	void showsLifecaresBeräkningInTheTabsDraftShape() {
		final var view = NormberakningMapper.toLifecareDraftView(forEdit(calculation(",\"householdSize\":2,\"amountForHouseholdSize\":1280,\"commonHouseholdCost\":1280")), "2026-09");

		assertThat(view.getSource()).isEqualTo("LIFECARE");
		assertThat(view.getFinalized()).isFalse();
		assertThat(view.getApplicationMonth()).isEqualTo("2026-09");
		assertThat(view.getNormId()).isEqualTo(1);
		assertThat(view.getNormTypeDisplayNames()).containsExactly("Riksnorm 2026");
		assertThat(view.getCalculationFromDate()).isEqualTo("2026-09-01");
		assertThat(view.getCalculationToDate()).isEqualTo("2026-09-30");
		assertThat(view.getCalculationDate()).isEqualTo("2026-09-24");
		assertThat(view.getHouseholdSize()).isEqualTo(2);
		assertThat(view.getHasCustomHouseholdSize()).isFalse();
		assertThat(view.getIncomeSum()).isEqualByComparingTo("5600");
		assertThat(view.getExpenseSum()).isEqualByComparingTo("5000");
		assertThat(view.getSpecialExpenseSum()).isEqualByComparingTo("0");
		assertThat(view.getAmountForHouseholdSize()).isEqualByComparingTo("1280");
		assertThat(view.getCommonHouseholdCost()).isEqualByComparingTo("1280");
		assertThat(view.getFamilyMembers()).isEqualTo(1);
		assertThat(view.getUpdated()).isEqualTo("2026-09-24");
		assertThat(view.getApplicantJobStimulus()).isFalse();
		assertThat(view.getPersons()).singleElement().satisfies(person -> {
			assertThat(person.getId()).isEqualTo("1");
			assertThat(person.getPersonalNumber()).isEqualTo("880209-T050");
			assertThat(person.getIncluded()).isTrue();
			assertThat(person.getAmount()).isEqualByComparingTo("3940");
			assertThat(person.getNormInterval()).isEqualTo("Ensamstående");
			assertThat(person.getNormRowId()).isEqualTo(2);
			assertThat(person.getRole()).isEqualTo("APPLICANT");
			assertThat(person.getOrigin()).isEqualTo("CASEWORKER");
			assertThat(person.getDeviationFromDate()).isNull();
		});
		assertThat(view.getIncomes()).singleElement().satisfies(income -> {
			assertThat(income.getId()).isEqualTo("19");
			assertThat(income.getTypeName()).isEqualTo("Aktivitetsstöd");
			assertThat(income.getApplicantCaseworkerAmount()).isEqualByComparingTo("5600");
			assertThat(income.getApplicantAmountDate()).isEqualTo("2026-06-17");
			assertThat(income.getCoapplicantAmountDate()).isNull();
			assertThat(income.getNote()).isNull();
			assertThat(income.getApplicantJobStimulus()).isNull();
		});
		assertThat(view.getExpenses()).extracting(NormberakningExpenseRow::getId, NormberakningExpenseRow::getBucket, NormberakningExpenseRow::getCostType,
			NormberakningExpenseRow::getAppliedAmount, NormberakningExpenseRow::getCaseworkerAmount)
			.containsExactly(tuple("E-3", "EXPENSE", "3", new BigDecimal("5000"), new BigDecimal("5000")));
		assertThat(view.getSpecialExpenses()).isEmpty();
	}

	@Test
	void showsTheGrossTheCaseworkerEnteredOnAnIncomeJobbstimulansAppliesTo() {
		final var saved = calculation(",\"hasApplicantJobStimuli\":true,\"normText\":\"\"");
		saved.set("calculationIncomes", tree("""
			[{"incomeCode":1,"incomeType":"Lön efter skatt","amountApplicant":3750,"grossAmountApplicant":5000,"amountCoApplicant":0,"applicantNote":"lön"},
			 {"incomeCode":19,"incomeType":"Aktivitetsstöd","amountApplicant":5600,"amountCoApplicant":0}]"""));

		final var view = NormberakningMapper.toLifecareDraftView(forEdit(saved), null);

		assertThat(view.getApplicantJobStimulus()).isTrue();
		assertThat(view.getNormTypeDisplayNames()).isEmpty();
		assertThat(view.getIncomes().get(0).getApplicantCaseworkerAmount()).isEqualByComparingTo("5000");
		assertThat(view.getIncomes().get(0).getApplicantJobStimulus()).isTrue();
		assertThat(view.getIncomes().get(0).getApplicantCountedAmount()).isEqualByComparingTo("3750");
		assertThat(view.getIncomes().get(0).getNote()).isEqualTo("lön");
		assertThat(view.getIncomes().get(1).getApplicantJobStimulus()).isNull();
	}

	@Test
	void leavesOutRowsLifecareDropsAndNamesRepeatedUtgifterApart() {
		final var saved = calculation("");
		saved.set("calculationIncomes", tree("[{\"incomeCode\":19,\"amountApplicant\":0,\"amountCoApplicant\":0}]"));
		saved.set("calculationExpenses", tree("""
			[{"expenseCode":3,"appliedAmount":5000,"approvedAmount":5000},{"expenseCode":3,"appliedAmount":0,"approvedAmount":0},
			 {"expenseCode":3,"appliedAmount":200,"approvedAmount":200,"note":"extra"}]"""));
		saved.set("calculationSpecialExpenses", tree("[{\"expenseCode\":7,\"appliedAmount\":1,\"approvedAmount\":0}]"));

		final var view = NormberakningMapper.toLifecareDraftView(forEdit(saved), null);

		assertThat(view.getIncomes()).isEmpty();
		assertThat(view.getExpenses()).extracting(NormberakningExpenseRow::getId, NormberakningExpenseRow::getPosition, NormberakningExpenseRow::getNote)
			.containsExactly(tuple("E-3", 0, null), tuple("E-3-3", 1, "extra"));
		assertThat(view.getSpecialExpenses()).extracting(NormberakningExpenseRow::getId).containsExactly("S-7");
	}

	@Test
	void countsABonusbarnAndAnyoneButTheApplicantUnder18AsABarn() {
		final var saved = calculation("");
		saved.set("calculationPersons", tree("""
			[{"personId":"a","personKey":1,"birthDate":"1988-02-09","included":true,"deviationDays":10},
			 {"personId":"b","personKey":2,"birthDate":"2014-12-01","included":true},
			 {"personId":"c","personKey":3,"birthDate":"2008-08-31","included":true},
			 {"personId":"d","personKey":4,"birthDate":"2010-01-01","isBonusChild":true,"included":true},
			 {"personId":"e","birthDate":"20100101","included":false}]"""));

		final var persons = NormberakningMapper.toLifecareDraftView(forEdit(saved), null).getPersons();

		assertThat(persons).extracting(NormberakningPersonRow::getId, NormberakningPersonRow::getRole)
			.containsExactly(tuple("1", "APPLICANT"), tuple("2", "CHILD"), tuple("3", null), tuple("4", "VISITATION_CHILD"), tuple("new-5", null));
		assertThat(persons.getFirst().getCaseworkerDays()).isEqualTo(10);
		assertThat(persons.getFirst().getEffectiveDays()).isEqualTo(10);
		assertThat(NormberakningMapper.roleOf(json("{\"birthDate\":\"2014-12-01\"}"), 1, "")).isNull();
	}

	@Test
	void namesTheNormRowsAsLifecaresListDoes() {
		final var saved = calculation(",\"norm\":{\"rows\":[{\"rowId\":1,\"name\":\"Make/maka/sambo\",\"monthlyAmount\":3550},{\"rowId\":2,\"name\":\"Ensamstående 3940.00\",\"monthlyAmount\":3940},{\"rowId\":3,\"name\":\"Utan belopp\"}]}");

		assertThat(NormberakningMapper.toLifecareDraftView(forEdit(saved), null).getNormRows()).extracting(NormberakningNormRow::getId, NormberakningNormRow::getName)
			.containsExactly(tuple(1, "Make/maka/sambo 3550.00"), tuple(2, "Ensamstående 3940.00"), tuple(3, "Utan belopp"));
	}

	@Test
	void signsLifecaresSummeringTheWayTheCaseworkerReadsIt() {
		final var saved = calculation("""
			,"calculationSummary":{"income":0,"jobStimulus":0,"jobStimulusDeduction":0,"norm":-5220,"expences":-100,"sum":-5220,"specialPurpose":0,
			  "balance":-5220,"deficitSum":5220,"commonHouseholdCost":1280,"familyCost":3940}""");

		final var view = NormberakningMapper.toLifecareCalculationView(saved);

		assertThat(view.getId()).isEqualTo(30);
		assertThat(view.getNormName()).isEqualTo("Riksnorm 2026");
		assertThat(view.getDate()).isEqualTo("2026-09-24");
		assertThat(view.getStartDate()).isEqualTo("2026-09-01");
		assertThat(view.getEndDate()).isEqualTo("2026-09-30");
		assertThat(view.getFinalized()).isFalse();
		assertThat(view.getUpdated()).isEqualTo("2026-09-24");
		assertThat(view.getSummary().getNorm()).isEqualByComparingTo("5220");
		assertThat(view.getSummary().getExpenses()).isEqualByComparingTo("100");
		assertThat(view.getSummary().getFamilyCost()).isEqualByComparingTo("3940");
		assertThat(view.getSummary().getCommonHouseholdCost()).isEqualByComparingTo("1280");
		assertThat(view.getSummary().getResult()).isEqualByComparingTo("-5220");
		assertThat(view.getSummary().getSum()).isEqualByComparingTo("-5220");
		assertThat(view.getSummary().getSpecialExpenses()).isEqualByComparingTo("0");
		assertThat(view.getSummary().getIncome()).isEqualByComparingTo("0");
		assertThat(view.getSummary().getJobStimulus()).isEqualByComparingTo("0");
		assertThat(view.getSummary().getJobStimulusDeduction()).isEqualByComparingTo("0");
		assertThat(NormberakningMapper.toLifecareCalculationView(calculation(",\"calculationSummary\":null")).getSummary()).isNull();
	}

	@Test
	void showsThePreviousBeräkningAsLifecareCountedItWithoutPersonnummer() {
		final var saved = json("""
			{"calculationId":1,"normId":1,"normText":"Riksnorm 2026","date":"2026-06-18","startDate":"2026-01-01","endDate":"2026-06-30",
			 "calculationSummary":{"income":12300,"norm":-26436,"balance":-19289,"familyCost":23640},
			 "calculationPersons":[{"personId":"19880209T050","name":"Testsson, Test","amount":23640,"included":true,"deviationFromDate":"","deviationToDate":"",
			   "personIdFormatted":"880209-T050"},{"personId":"x","name":"Utanför","included":false}],
			 "calculationIncomes":[{"incomeCode":19,"incomeType":"Aktivitetsstöd","amountApplicant":5600,"applicantSearchDate":"2026-06-17","amountCoApplicant":0,
			   "coApplicantSearchDate":""}],
			 "calculationExpenses":[{"expenseCode":3,"expenseType":"Boendekostnad","appliedAmount":5000,"approvedAmount":5000,"note":""}],
			 "calculationSpecialExpenses":[{"expenseCode":3,"expenseType":"Glasögon","appliedAmount":1000,"approvedAmount":1000,"note":""}],
			 "hasCustomHouseholdSize":true,"isFinalized":false,"updateTimestamp":"2026-09-10","sumInk":12300,"sumUtg":5153,"sumSpec":1000,"sumNorm":26436,
			 "totSum":-20289,"commonHouseholdCost":2796}""");

		final var previous = NormberakningMapper.toPreviousCalculation(saved);

		assertThat(previous.getId()).isEqualTo(1);
		assertThat(previous.getNorm()).isEqualTo("Riksnorm 2026");
		assertThat(previous.getFromDate()).isEqualTo("2026-01-01");
		assertThat(previous.getToDate()).isEqualTo("2026-06-30");
		assertThat(previous.getIncomeSum()).isEqualByComparingTo("12300");
		assertThat(previous.getExpenseSum()).isEqualByComparingTo("5153");
		assertThat(previous.getSpecialExpenseSum()).isEqualByComparingTo("1000");
		assertThat(previous.getNormSum()).isEqualByComparingTo("26436");
		assertThat(previous.getCommonHouseholdCost()).isEqualByComparingTo("2796");
		assertThat(previous.getFamilyCost()).isEqualByComparingTo("23640");
		assertThat(previous.getBalance()).isEqualByComparingTo("-19289");
		assertThat(previous.getTotalSum()).isEqualByComparingTo("-20289");
		assertThat(previous.getIsFinal()).isFalse();
		assertThat(previous.getPersons()).containsExactly(NormberakningPreviousPerson.create().withName("Testsson, Test").withAmount(new BigDecimal("23640")));
		assertThat(previous.getIncomes()).singleElement().satisfies(income -> {
			assertThat(income.getType()).isEqualTo("Aktivitetsstöd");
			assertThat(income.getApplicantSearchDate()).isEqualTo("2026-06-17");
			assertThat(income.getCoApplicantSearchDate()).isNull();
			assertThat(income.getAmountApplicant()).isEqualByComparingTo("5600");
			assertThat(income.getAmountCoApplicant()).isEqualByComparingTo("0");
		});
		assertThat(previous.getExpenses()).singleElement().satisfies(expense -> assertThat(expense.getType()).isEqualTo("Boendekostnad"));
		assertThat(previous.getSpecialExpenses()).singleElement().satisfies(expense -> assertThat(expense.getApprovedAmount()).isEqualByComparingTo("1000"));
	}

	@Test
	void showsCaremsDraftWithEachPersonsPersonnummer() {
		final var created = OffsetDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final var draft = CalculationDraft.create()
			.withErrandId("e1")
			.withApplicationMonth("2026-09")
			.withNormId(1)
			.withNormType(List.of("NATIONAL_NORM"))
			.withNormTypeDisplayNames(List.of("Riksnorm"))
			.withCalculationFromDate(LocalDate.of(2026, 9, 1))
			.withCalculationToDate(LocalDate.of(2026, 9, 30))
			.withCalculationDate(LocalDate.of(2026, 9, 24))
			.withHasCustomHouseholdSize(true)
			.withHouseholdSize(3)
			.withPersons(List.of(NormPersonRow.create().withId("r1").withPartyId("p1").withRole("APPLICANT").withIncluded(true)
				.withDeviationFromDate(LocalDate.of(2026, 9, 5)), NormPersonRow.create().withId("r2")))
			.withIncomes(List.of(NormIncomeRow.create().withId("i1").withApplicantAmountDate(created).withApplicantEffectiveAmount(BigDecimal.TEN)))
			.withExpenses(List.of(NormExpenseRow.create().withId("x1").withBucket("EXPENSE").withCostType("RENT").withProcessAmount(BigDecimal.ONE)))
			.withSpecialExpenses(null)
			.withIncomeSum(BigDecimal.TEN)
			.withCreated(created)
			.withUpdated(created);

		final var view = NormberakningMapper.toCaremDraftView(draft, Map.of("p1", "19880209T050"));

		assertThat(view.getSource()).isEqualTo("CAREM");
		assertThat(view.getErrandId()).isEqualTo("e1");
		assertThat(view.getCalculationFromDate()).isEqualTo("2026-09-01");
		assertThat(view.getCalculationDate()).isEqualTo("2026-09-24");
		assertThat(view.getCreated()).isEqualTo("2026-09-01T00:00:00Z");
		assertThat(view.getHouseholdSize()).isEqualTo(3);
		assertThat(view.getNormType()).containsExactly("NATIONAL_NORM");
		assertThat(view.getPersons()).extracting(NormberakningPersonRow::getId, NormberakningPersonRow::getPersonalNumber, NormberakningPersonRow::getDeviationFromDate)
			.containsExactly(tuple("r1", "19880209T050", "2026-09-05"), tuple("r2", null, null));
		assertThat(view.getIncomes().getFirst().getApplicantAmountDate()).isEqualTo("2026-09-01T00:00:00Z");
		assertThat(view.getExpenses().getFirst().getProcessAmount()).isEqualByComparingTo("1");
		assertThat(view.getSpecialExpenses()).isEmpty();
		assertThat(view.getFinalized()).isNull();
	}

	@Test
	void offersLifecaresActiveTypesAndNorms() {
		final var forEdit = forEdit(calculation(""));

		assertThat(NormberakningMapper.toTypeOptions(forEdit.path("incomeTypes"))).extracting(NormberakningTypeOption::getCode, NormberakningTypeOption::getDisplayName)
			.containsExactly(tuple("19", "Aktivitetsstöd"), tuple("1", "Lön efter skatt"));
		assertThat(NormberakningMapper.toNormOptions(forEdit.path("norms"))).extracting(NormberakningTypeOption::getCode).containsExactly("1");
	}

	@Test
	void offersCaremsTypesWithTheLifecareLabelFirst() {
		assertThat(NormberakningMapper.toTypeOption(TypeOption.create().withCode("RENT").withInternalDisplayName("Boendekostnad").withExternalDisplayName("Hyra")).getDisplayName())
			.isEqualTo("Boendekostnad");
		assertThat(NormberakningMapper.toTypeOption(TypeOption.create().withCode("RENT").withExternalDisplayName("Hyra")).getDisplayName()).isEqualTo("Hyra");
		assertThat(NormberakningMapper.toTypeOption(TypeOption.create().withCode("RENT")).getDisplayName()).isEqualTo("RENT");
	}

	@Test
	void mapsARowInputToCaremsSectionInputs() {
		final var input = NormberakningRowInput.create()
			.withTypeId(19).withTypeName("Aktivitetsstöd").withApplicantCaseworkerAmount(BigDecimal.TEN).withApplicantAmountDate("2026-09-02T00:00:00+02:00")
			.withCoapplicantCaseworkerAmount(BigDecimal.ONE).withCoapplicantAmountDate("2026-09-03T00:00:00Z")
			.withCostType("RENT").withBucket("EXPENSE").withOtherSubType("x").withSpecification("spec").withAppliedAmount(BigDecimal.TWO).withCaseworkerAmount(BigDecimal.ONE)
			.withPartyId("p1").withRole("CHILD").withName("Barn").withCaseworkerDays(5).withIncluded(true).withDeviationFromDate("2026-09-01").withDeviationToDate("2026-09-10")
			.withNormInterval("Barn 7-10").withNote("note");

		final var income = NormberakningMapper.toIncomeInput(input);
		final var expense = NormberakningMapper.toExpenseInput(input);
		final var person = NormberakningMapper.toPersonInput(input);

		assertThat(income.getApplicantAmountDate()).isEqualTo(OffsetDateTime.parse("2026-09-02T00:00:00+02:00"));
		assertThat(income.getCoapplicantAmountDate()).isEqualTo(OffsetDateTime.parse("2026-09-03T00:00:00Z"));
		assertThat(income.getTypeName()).isEqualTo("Aktivitetsstöd");
		assertThat(income.getNote()).isEqualTo("note");
		assertThat(expense.getCostType()).isEqualTo("RENT");
		assertThat(expense.getAppliedAmount()).isEqualTo(BigDecimal.TWO);
		assertThat(expense.getSpecification()).isEqualTo("spec");
		assertThat(person.getDeviationFromDate()).isEqualTo(LocalDate.of(2026, 9, 1));
		assertThat(person.getDeviationToDate()).isEqualTo(LocalDate.of(2026, 9, 10));
		assertThat(person.getIncluded()).isTrue();
		assertThat(person.getNormInterval()).isEqualTo("Barn 7-10");
		assertThat(NormberakningMapper.toIncomeInput(NormberakningRowInput.create()).getApplicantAmountDate()).isNull();
		assertThat(NormberakningMapper.toPersonInput(NormberakningRowInput.create()).getDeviationFromDate()).isNull();
	}

	@Test
	void refusesDatesCaremsDraftCannotRead() {
		assertThatThrownBy(() -> NormberakningMapper.toIncomeInput(NormberakningRowInput.create().withApplicantAmountDate("2026-09-02")))
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
		assertThatThrownBy(() -> NormberakningMapper.toPersonInput(NormberakningRowInput.create().withDeviationFromDate("2026-09-02T00:00:00Z")))
			.hasFieldOrPropertyWithValue("status", BAD_REQUEST);
	}
}
