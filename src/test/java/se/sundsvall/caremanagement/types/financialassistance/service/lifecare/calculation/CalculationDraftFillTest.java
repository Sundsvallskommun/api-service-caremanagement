package se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.types.financialassistance.api.model.CalculationDraft;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormExpenseRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormIncomeRow;
import se.sundsvall.caremanagement.types.financialassistance.api.model.NormPersonRow;
import se.sundsvall.dept44.problem.ThrowableProblem;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationDraftFill.applyDraft;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationDraftFill.identityKey;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.blank;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.catalogues;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.draft;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.CalculationFixtures.personsOf;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.calculation.LifecareJson.objects;

class CalculationDraftFillTest {

	private static final String TODAY = "2026-09-24";
	private static final String APPLICANT_NUMBER = "880209-T050";
	private static final String CHILD_NUMBER = "141201-T010";

	private static ObjectNode fill(final CalculationDraft draft, final String... numbers) {
		return applyDraft(blank(), List.of(), draft, personsOf(draft, numbers), catalogues(), TODAY);
	}

	@Test
	void fillsTheUnderlagFromTheDraft() {
		final var calculation = fill(draft(), APPLICANT_NUMBER, CHILD_NUMBER);

		assertThat(calculation.path("startDate").stringValue()).isEqualTo("2026-09-01");
		assertThat(calculation.path("endDate").stringValue()).isEqualTo("2026-09-30");
		assertThat(calculation.path("date").stringValue()).isEqualTo(TODAY);
		assertThat(calculation.path("normId").intValue()).isEqualTo(1);
		assertThat(objects(calculation, "calculationPersons"))
			.extracting(member -> member.path("name").stringValue(), member -> member.path("included").booleanValue())
			.containsExactly(tuple("Testsson, Test", true), tuple("Testbarn Test, Testar", false));
		// An income type the underlag lacks gets a row of its own; the default one stays at 0.
		assertThat(objects(calculation, "calculationIncomes"))
			.extracting(row -> row.path("incomeCode").intValue(), row -> row.path("amountApplicant").doubleValue())
			.containsExactly(tuple(1, 0.0), tuple(27, 1500.0));
		assertThat(objects(calculation, "calculationIncomes").get(1).path("amountApplicant").isInt()).isTrue();
		assertThat(objects(calculation, "calculationExpenses"))
			.extracting(row -> row.path("expenseType").stringValue(), row -> row.path("appliedAmount").doubleValue(), row -> row.path("approvedAmount").doubleValue(),
				row -> row.path("note").stringValue(null))
			.containsExactly(tuple("A-kasseavgift", 454.0, 454.0, "jkljkl"), tuple("Boendekostnad", 6000.0, 5500.0, null));
		assertThat(objects(calculation, "calculationSpecialExpenses"))
			.extracting(row -> row.path("expenseType").stringValue(), row -> row.path("approvedAmount").doubleValue())
			.containsExactly(tuple("Tandvård", 300.0));
		assertThat(calculation.path("hasCustomHouseholdSize").booleanValue()).isFalse();
	}

	@Test
	void leavesTheBaseUntouched() {
		final var base = blank();
		final var before = base.deepCopy();

		applyDraft(base, List.of(), draft(), personsOf(draft(), APPLICANT_NUMBER), catalogues(), TODAY);

		assertThat(base).isEqualTo(before);
	}

	@Test
	void findsAnIncomeByNameWhenCaremsCodeIsNotLifecares() {
		final var draft = draft().withIncomes(List.of(NormIncomeRow.create().withTypeId(9).withTypeName("lön efter skatt ").withApplicantEffectiveAmount(BigDecimal.valueOf(5000))));

		final var calculation = fill(draft, APPLICANT_NUMBER);

		assertThat(objects(calculation, "calculationIncomes"))
			.extracting(row -> row.path("incomeCode").intValue(), row -> row.path("amountApplicant").doubleValue())
			.containsExactly(tuple(1, 5000.0));
	}

	@Test
	void findsAnIncomeByIdAndAddsUpRowsOfTheSameType() {
		final var draft = draft().withIncomes(List.of(
			NormIncomeRow.create().withTypeId(27).withApplicantEffectiveAmount(BigDecimal.valueOf(100)),
			NormIncomeRow.create().withTypeId(27).withApplicantEffectiveAmount(BigDecimal.valueOf(50)).withCoapplicantEffectiveAmount(BigDecimal.valueOf(20)),
			NormIncomeRow.create().withTypeId(27).withApplicantEffectiveAmount(BigDecimal.valueOf(900)).withDeleted(true),
			NormIncomeRow.create().withTypeId(27)));

		final var calculation = fill(draft, APPLICANT_NUMBER);

		assertThat(objects(calculation, "calculationIncomes").get(1).path("amountApplicant").doubleValue()).isEqualTo(150.0);
		assertThat(objects(calculation, "calculationIncomes").get(1).path("amountCoApplicant").doubleValue()).isEqualTo(20.0);
	}

	@Test
	void addsUpExpensesOfTheSameTypeJoiningTheirNotes() {
		final var draft = draft().withSpecialExpenses(List.of()).withExpenses(List.of(
			NormExpenseRow.create().withCostTypeDisplayName("Boendekostnad").withEffectiveAmount(BigDecimal.valueOf(100)).withNote("hyra"),
			NormExpenseRow.create().withCostType("Boendekostnad").withEffectiveAmount(BigDecimal.valueOf(50)).withAppliedAmount(BigDecimal.valueOf(60)),
			NormExpenseRow.create().withCostTypeDisplayName("Boendekostnad").withEffectiveAmount(BigDecimal.valueOf(10)).withSpecification("el"),
			NormExpenseRow.create().withCostTypeDisplayName("A-kasseavgift").withEffectiveAmount(BigDecimal.ZERO),
			NormExpenseRow.create().withBucket("SPECIAL_EXPENSE").withCostTypeDisplayName("Tandvård").withEffectiveAmount(BigDecimal.valueOf(5))));

		final var calculation = fill(draft, APPLICANT_NUMBER);

		final var housing = objects(calculation, "calculationExpenses").get(1);
		assertThat(housing.path("appliedAmount").doubleValue()).isEqualTo(170.0);
		assertThat(housing.path("approvedAmount").doubleValue()).isEqualTo(160.0);
		assertThat(housing.path("note").stringValue()).isEqualTo("hyra; el");
		assertThat(objects(calculation, "calculationSpecialExpenses")).extracting(row -> row.path("approvedAmount").doubleValue()).containsExactly(5.0);
	}

	@Test
	void takesTheApplicantAsLifecaresFirstMemberWhenNoPersonnummerMatches() {
		final var calculation = fill(draft());

		assertThat(objects(calculation, "calculationPersons").getFirst().path("included").booleanValue()).isTrue();
	}

	@Test
	void bringsBackAMemberASavedBeräkningLeftOut() {
		final var saved = blank();
		saved.put("calculationId", 31);
		final var applicant = (ObjectNode) saved.path("calculationPersons").get(0);
		final var child = (ObjectNode) saved.path("calculationPersons").get(1);
		applicant.put("included", true);
		saved.set("calculationPersons", LifecareJson.array(List.of(applicant)));
		final var draft = draft();
		draft.getPersons().forEach(person -> person.setIncluded(true));

		final var calculation = applyDraft(saved, List.of(applicant, child), draft, personsOf(draft, APPLICANT_NUMBER, CHILD_NUMBER), catalogues(), TODAY);

		assertThat(objects(calculation, "calculationPersons"))
			.extracting(member -> member.path("personId").stringValue(), member -> member.path("included").booleanValue())
			.containsExactly(tuple("19880209T050", true), tuple("20141201T010", true));
	}

	@Test
	void refusesARowWhoseTypeLifecareDoesNotKnow() {
		final var draft = draft().withExpenses(List.of(NormExpenseRow.create().withBucket("EXPENSE").withCostTypeDisplayName("Påhittad kostnad").withEffectiveAmount(BigDecimal.TEN)))
			.withIncomes(List.of(NormIncomeRow.create().withTypeId(99).withApplicantEffectiveAmount(BigDecimal.ONE), NormIncomeRow.create().withTypeId(99).withApplicantEffectiveAmount(BigDecimal.ONE)));

		assertThatThrownBy(() -> fill(draft, APPLICANT_NUMBER))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("Lifecare känner inte till: 99, Påhittad kostnad.");
	}

	@Test
	void refusesADraftWithoutAPeriod() {
		assertThatThrownBy(() -> fill(draft().withCalculationToDate(null), APPLICANT_NUMBER))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("saknar period");
		assertThatThrownBy(() -> fill(draft().withCalculationFromDate(null), APPLICANT_NUMBER))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT);
	}

	@Test
	void refusesAHouseholdWithAnIncludedCoApplicant() {
		final var draft = draft();
		draft.getPersons().add(NormPersonRow.create().withRole("CO_APPLICANT").withName("Medsökande").withIncluded(true));

		assertThatThrownBy(() -> fill(draft, APPLICANT_NUMBER))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("medsökande");
	}

	@Test
	void ignoresADeletedCoApplicant() {
		final var draft = draft();
		draft.getPersons().add(NormPersonRow.create().withRole("CO_APPLICANT").withIncluded(true).withDeleted(true));

		assertThat(fill(draft, APPLICANT_NUMBER).path("startDate").stringValue()).isEqualTo("2026-09-01");
	}

	@Test
	void refusesWhenNoOneIsIncluded() {
		final var draft = draft();
		draft.getPersons().forEach(person -> person.setIncluded(false));

		assertThatThrownBy(() -> fill(draft, APPLICANT_NUMBER))
			.hasFieldOrPropertyWithValue("status", UNPROCESSABLE_CONTENT)
			.hasMessageContaining("Ingen i hushållet");
	}

	@Test
	void keepsLifecaresNormWhenTheDraftsIsNotInTheCatalogue() {
		assertThat(fill(draft().withNormId(99), APPLICANT_NUMBER).path("normId").intValue()).isEqualTo(1);
		assertThat(fill(draft().withNormId(3), APPLICANT_NUMBER).path("normId").intValue()).isEqualTo(3);
		assertThat(fill(draft().withNormId(null), APPLICANT_NUMBER).path("normId").intValue()).isEqualTo(1);
	}

	@Test
	void toleratesADraftWithoutRows() {
		final var draft = draft().withIncomes(null).withExpenses(null).withSpecialExpenses(null).withHasCustomHouseholdSize(null);

		final JsonNode calculation = fill(draft, APPLICANT_NUMBER);

		assertThat(objects(calculation, "calculationIncomes")).hasSize(1);
		assertThat(calculation.path("hasCustomHouseholdSize").booleanValue()).isFalse();
	}

	@Test
	void reducesIdentitiesToWhatTwoSpellingsShare() {
		assertThat(identityKey("19880209T050")).isEqualTo(identityKey("880209-T050")).isEqualTo("880209T050");
		assertThat(identityKey(null)).isEmpty();
	}
}
