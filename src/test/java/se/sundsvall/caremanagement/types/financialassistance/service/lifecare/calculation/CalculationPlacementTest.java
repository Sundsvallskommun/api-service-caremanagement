package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationBodyBuilder.HouseholdSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.json;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.tree;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationJson.objects;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.householdSizeOf;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.householdSizeOfSaved;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.normRowName;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.withJobStimulusIncomes;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.withNormRowNames;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationPlacement.withPlacedPersons;

class CalculationPlacementTest {

	private static final String TYPES = """
		[{"id":1,"text":"Lön efter skatt","isActive":true,"isJobStimulus":true,"jobStimulusPercent":25},
		 {"id":19,"text":"Aktivitetsstöd","isActive":true},
		 {"id":5,"text":"Utan procent","isActive":true,"isJobStimulus":true}]""";

	@Test
	void countsJobbstimulansOnTheLönAsTheWebAppDoes() {
		final var calculation = json("""
			{"hasApplicantJobStimuli":true,"calculationIncomes":[
			  {"incomeCode":1,"amountApplicant":5000,"amountCoApplicant":0},
			  {"incomeCode":1,"amountApplicant":0},
			  {"incomeCode":19,"amountApplicant":800},
			  {"incomeCode":5,"amountApplicant":800},
			  {"incomeCode":1,"amountApplicant":333}]}""");

		final var rows = objects(withJobStimulusIncomes(calculation, tree(TYPES)), "calculationIncomes");

		assertThat(rows.get(0).path("amountApplicant").intValue()).isEqualTo(3750);
		assertThat(rows.get(0).path("amountApplicant").isInt()).isTrue();
		assertThat(rows.get(0).path("grossAmountApplicant").intValue()).isEqualTo(5000);
		assertThat(rows.get(0).path("grossAmountCoApplicant").intValue()).isZero();
		assertThat(rows.get(1).path("grossAmountApplicant").intValue()).isZero();
		assertThat(rows.get(2).path("amountApplicant").intValue()).isEqualTo(800);
		assertThat(rows.get(2).path("grossAmountApplicant").intValue()).isEqualTo(800);
		assertThat(rows.get(3).path("amountApplicant").intValue()).isEqualTo(800);
		assertThat(rows.get(4).path("amountApplicant").doubleValue()).isEqualTo(249.75);
	}

	@Test
	void countsTheWholeLönWithoutJobbstimulansInThePeriod() {
		final var calculation = json("""
			{"hasApplicantJobStimuli":false,"calculationIncomes":[{"incomeCode":1,"amountApplicant":5000}]}""");

		assertThat(objects(withJobStimulusIncomes(calculation, tree(TYPES)), "calculationIncomes").getFirst().path("amountApplicant").intValue()).isEqualTo(5000);
	}

	@Test
	void sendsEveryIncomeWithItsAmountAsGross() {
		// Lifecare takes gross minus amount as the jobbstimulans deduction: a row left at gross 0 turns the whole income
		// into a deduction in its summering (EB-26090046, 2026-09-25).
		final var calculation = json("""
			{"hasApplicantJobStimuli":false,"calculationIncomes":[
			  {"incomeCode":19,"amountApplicant":9800,"grossAmountApplicant":0,"amountCoApplicant":1200,"grossAmountCoApplicant":0},
			  {"incomeCode":1,"amountApplicant":1000,"grossAmountApplicant":0}]}""");

		final var rows = objects(withJobStimulusIncomes(calculation, tree(TYPES)), "calculationIncomes");

		assertThat(rows).extracting(row -> row.path("grossAmountApplicant").intValue(), row -> row.path("grossAmountCoApplicant").intValue())
			.containsExactly(tuple(9800, 1200), tuple(1000, 0));
	}

	@Test
	void keepsAPlacementTheCaseworkerMadeAndPlacesTheRest() {
		final var calculation = json("""
			{"calculationPersons":[
			  {"personId":"a","included":true,"normRowId":2,"normRow":"Ensamstående","amount":3940},
			  {"personId":"b","included":true,"normRowId":0,"normRow":null,"amount":0},
			  {"personId":"c","included":false,"normRowId":7,"normRow":"Barn","amount":100},
			  {"personId":"d","included":true}]}""");
		final var placed = List.of(json("{\"personId\":\"a\",\"normRowId\":1,\"normRow\":\"Make\",\"amount\":3550}"),
			json("{\"personId\":\"b\",\"normRowId\":8,\"normRow\":\"Barn 7-10\",\"amount\":2342}"));

		final var persons = objects(withPlacedPersons(calculation, placed, true), "calculationPersons");

		assertThat(persons.get(0).path("normRowId").intValue()).isEqualTo(2);
		assertThat(persons.get(0).path("amount").intValue()).isEqualTo(3940);
		assertThat(persons.get(1).path("normRowId").intValue()).isEqualTo(8);
		assertThat(persons.get(1).path("amount").intValue()).isEqualTo(2342);
		assertThat(persons.get(2)).isEqualTo(json("{\"personId\":\"c\",\"included\":false,\"normRowId\":0,\"normRow\":null,\"amount\":0}"));
		assertThat(persons.get(3)).isEqualTo(json("{\"personId\":\"d\",\"included\":true,\"normRowId\":0,\"normRow\":null,\"amount\":0}"));
	}

	@Test
	void takesWhatLifecarePlacedOnANewNorm() {
		final var calculation = json("""
			{"calculationPersons":[{"personId":"a","included":true,"normRowId":12,"normRow":"Barn 11-14","amount":4390}]}""");

		final var persons = objects(withPlacedPersons(calculation, List.of(json("{\"personId\":\"a\",\"normRowId\":8,\"amount\":2342}")), false), "calculationPersons");

		assertThat(persons.getFirst().path("normRowId").intValue()).isEqualTo(8);
		assertThat(persons.getFirst().path("amount").intValue()).isEqualTo(2342);
		// Lifecare answered no name: JSON leaves the undefined out.
		assertThat(persons.getFirst().has("normRow")).isFalse();
	}

	@Test
	void namesEachPlacedMembersNormintervall() {
		final var calculation = json("""
			{"norm":{"rows":[{"rowId":1,"name":"Make/maka/sambo 4380.00"}]},"calculationPersons":[
			  {"personId":"a","normRowId":1,"normRow":null},
			  {"personId":"b","normRowId":1,"normRow":"Egen"},
			  {"personId":"c","normRowId":0},
			  {"personId":"d","normRowId":9}]}""");

		final var persons = objects(withNormRowNames(calculation), "calculationPersons");

		assertThat(persons.get(0).path("normRow").stringValue()).isEqualTo("Make/maka/sambo");
		assertThat(persons.get(1).path("normRow").stringValue()).isEqualTo("Egen");
		assertThat(persons.get(2).has("normRow")).isFalse();
		assertThat(persons.get(3).has("normRow")).isFalse();
	}

	@Test
	void namesANormRowWithoutItsAmount() {
		assertThat(normRowName("Ensamstående 3940.00")).isEqualTo("Ensamstående");
		assertThat(normRowName("Barn 7-10 år 1947,5")).isEqualTo("Barn 7-10 år");
		assertThat(normRowName("Barn 11-14")).isEqualTo("Barn 11-14");
	}

	@Test
	void takesTheHouseholdSizeFromTheDraftOrTheMembers() {
		final var calculation = json("{\"calculationPersons\":[{\"included\":true},{\"included\":false},{\"included\":true}]}");

		assertThat(householdSizeOf(calculation, true, 4)).isEqualTo(new HouseholdSize(true, 4, 2));
		assertThat(householdSizeOf(calculation, true, null)).isEqualTo(new HouseholdSize(false, 2, 2));
		assertThat(householdSizeOf(calculation, null, 4)).isEqualTo(new HouseholdSize(false, 2, 2));
	}

	@Test
	void takesTheHouseholdSizeASavedBeräkningHolds() {
		assertThat(householdSizeOfSaved(json("{\"hasCustomHouseholdSize\":true,\"householdSize\":5,\"calculationPersons\":[{\"included\":true}]}")))
			.isEqualTo(new HouseholdSize(true, 5, 1));
		assertThat(householdSizeOfSaved(json("{\"hasCustomHouseholdSize\":true,\"calculationPersons\":[{\"included\":true}]}")))
			.isEqualTo(new HouseholdSize(false, 1, 1));
		assertThat(householdSizeOfSaved(json("{\"hasCustomHouseholdSize\":false,\"householdSize\":5,\"calculationPersons\":[]}")))
			.isEqualTo(new HouseholdSize(false, 0, 0));
	}
}
