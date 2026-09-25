package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationExpenseView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationIncomeView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationPersonView;
import se.sundsvall.caremanagement.lifecare.service.model.CalculationView;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.model.FaWarningEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SavedCalculationWarningsTest {

	private static final CalculationView CALCULATION = new CalculationView(31, null, null, null, null, null, null, null, null, null, null, null, false,
		List.of(new CalculationPersonView(null, "Anna Andersson", BigDecimal.ONE, null, null)),
		List.of(
			new CalculationIncomeView("Barnbidrag", new BigDecimal("1250"), null, null, null),
			new CalculationIncomeView("Dagersättning från FK", null, null, new BigDecimal("9800"), null),
			// A row left at zero is no income.
			new CalculationIncomeView("A-kassa", BigDecimal.ZERO, null, null, null),
			new CalculationIncomeView("Lön efter skatt", new BigDecimal("6000"), null, null, null),
			new CalculationIncomeView("lön efter skatt ", new BigDecimal("500"), null, null, null)),
		List.of(new CalculationExpenseView("Hemförsäkring", new BigDecimal("200"), new BigDecimal("200"))),
		List.of(new CalculationExpenseView("Tandvård", new BigDecimal("2000"), new BigDecimal("1500"))));

	private static FaWarningEntity warning(final String type, final String sourceKey) {
		return FaWarningEntity.create().withType(type).withSourceKey(sourceKey).withMessage("msg");
	}

	@Test
	void newIncomeIsResolvedOnceTheCalculationHasIt() {
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_NEW_INCOME, "Barnbidrag"), CALCULATION)).isTrue();
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_NEW_INCOME, "dagersättning från fk"), CALCULATION)).isTrue();
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_NEW_INCOME, "A-kassa"), CALCULATION)).isFalse();
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_NEW_INCOME, "Underhållsstöd"), CALCULATION)).isFalse();
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_NEW_INCOME, ""), CALCULATION)).isFalse();
	}

	@Test
	void droppedIncomeIsResolvedOnceTheCalculationNoLongerHasIt() {
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_INCOME_DROPPED, "A-kassa"), CALCULATION)).isTrue();
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_INCOME_DROPPED, "Barnbidrag"), CALCULATION)).isFalse();
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_INCOME_DROPPED, null), CALCULATION)).isFalse();
	}

	@Test
	void newExpenseIsResolvedOnceEitherBucketHasItsType() {
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_NEW_EXPENSE, "Hemförsäkring – Länsförsäkringar"), CALCULATION)).isTrue();
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_NEW_EXPENSE, "Tandvård"), CALCULATION)).isTrue();
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_NEW_EXPENSE, "Glasögon"), CALCULATION)).isFalse();
	}

	@Test
	void newPersonIsResolvedOnlyOnTheSameName() {
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_NEW_PERSON, "Anna Andersson"), CALCULATION)).isTrue();
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_NEW_PERSON, "Bo Andersson"), CALCULATION)).isFalse();
	}

	@Test
	void duplicatedIncomeIsResolvedWhenOneRowIsLeft() {
		final var twice = warning(WarningService.TYPE_INCOME_DUPLICATED, "income-duplicate:20")
			.withMessage("Möjlig dubbelföring: Lön efter skatt finns både från SSBTEK och tillagd av handläggare — kontrollera att inkomsten inte räknas två gånger");
		final var once = warning(WarningService.TYPE_INCOME_DUPLICATED, "income-duplicate:21")
			.withMessage("Möjlig dubbelföring: Barnbidrag finns både från SSBTEK och tillagd av handläggare — kontrollera att inkomsten inte räknas två gånger");
		final var unreadable = warning(WarningService.TYPE_INCOME_DUPLICATED, "income-duplicate:22").withMessage("Något annat");

		assertThat(SavedCalculationWarnings.resolved(twice, CALCULATION)).isFalse();
		assertThat(SavedCalculationWarnings.resolved(once, CALCULATION)).isTrue();
		assertThat(SavedCalculationWarnings.resolved(unreadable, CALCULATION)).isFalse();
	}

	@Test
	void whatTheCalculationCannotTellIsLeftOpen() {
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_INCOME_NOT_TRANSFERABLE, "Studiemedel"), CALCULATION)).isFalse();
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_PREVIOUS_NORM_NOT_AVAILABLE, "previous-norm"), CALCULATION)).isFalse();
		assertThat(SavedCalculationWarnings.resolved(warning(null, "Barnbidrag"), CALCULATION)).isFalse();
	}

	@Test
	void anEmptyCalculationSettlesOnlyADroppedIncome() {
		final var empty = new CalculationView(31, null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, null);

		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_NEW_INCOME, "Barnbidrag"), empty)).isFalse();
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_NEW_EXPENSE, "Tandvård"), empty)).isFalse();
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_NEW_PERSON, "Anna Andersson"), empty)).isFalse();
		assertThat(SavedCalculationWarnings.resolved(warning(WarningService.TYPE_INCOME_DROPPED, "Barnbidrag"), empty)).isTrue();
	}
}
