package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.caremanagement.lifecare.service.model.IncomeTypeTotal;
import se.sundsvall.caremanagement.operaton.service.ProcessService;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@ExtendWith(MockitoExtension.class)
class IncomeChangeFeederTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String RULE = "Bostadsbidrag föregående månad är inte samma summa som denna månad – kontrollera summan";
	private static final Function<IncomeTypeTotal, IncomeChangeFeeder.Threshold> EXACT = total -> new IncomeChangeFeeder.Threshold(0, RULE);
	private static final Function<IncomeTypeTotal, IncomeChangeFeeder.Threshold> DEFAULT = total -> IncomeChangeFeeder.Threshold.defaults();
	private static final Function<IncomeTypeTotal, IncomeChangeFeeder.Threshold> NO_COMPARISON = total -> new IncomeChangeFeeder.Threshold(-1, null);

	@Mock
	private ProcessService processServiceMock;

	@InjectMocks
	private IncomeChangeFeeder feeder;

	private static IncomeTypeTotal total(final String typeName, final String amount, final String... benefits) {
		return new IncomeTypeTotal(typeName, new BigDecimal(amount), List.of(benefits));
	}

	private static Optional<Map<String, BigDecimal>> previous(final String normalisedType, final String amount) {
		return Optional.of(Map.of(normalisedType, new BigDecimal(amount)));
	}

	@Test
	void exactThresholdWarnsOnAnyDifferenceWithTheRuleText() {
		final var warnings = IncomeChangeFeeder.changeWarnings(List.of(total("Bostadsbidrag", "1255.00", "Bostadsbidrag")),
			previous("bostadsbidrag", "1250"), EXACT);

		// 0,4 % — a rounded percent would call it no change; the exact threshold compares the amounts.
		assertThat(warnings).containsExactly("Bostadsbidrag: 1250 kr i föregående normberäkning → 1255 kr nu – " + RULE);
	}

	@Test
	void exactThresholdIsQuietForTheSameAmountWhateverItsScale() {
		assertThat(IncomeChangeFeeder.changeWarnings(List.of(total("Bostadsbidrag", "1250.00", "Bostadsbidrag")),
			previous("bostadsbidrag", "1250"), EXACT)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({
		"1000, 1120, false", // exactly 12 % is within
		"1000, 880, false",
		"1000, 1121, true",
		"1000, 879, true",
		"1000, 0, true"
	})
	void percentThresholdWarnsOnlyBeyondTheTolerance(final String previousAmount, final String currentAmount, final boolean warns) {
		final var warnings = IncomeChangeFeeder.changeWarnings(List.of(total("Lön efter skatt", currentAmount, "Lön")),
			previous("lön efter skatt", previousAmount), DEFAULT);

		if (warns) {
			// No rule text from the default: the sentence ends with the amounts.
			assertThat(warnings).containsExactly("Lön efter skatt: " + previousAmount + " kr i föregående normberäkning → " + currentAmount + " kr nu");
		} else {
			assertThat(warnings).isEmpty();
		}
	}

	@Test
	void noComparisonThresholdSkipsBothChangedAndNewIncomes() {
		final var warnings = IncomeChangeFeeder.changeWarnings(List.of(total("Bostadsbidrag", "2000", "Bostadsbidrag"), total("Barnbidrag/Flerbarnstillägg", "1250", "Allmänt barnbidrag")),
			previous("bostadsbidrag", "1000"), NO_COMPARISON);

		assertThat(warnings).isEmpty();
	}

	@Test
	void aTypeThePreviousCalculationLackedIsANewIncome() {
		final var warnings = IncomeChangeFeeder.changeWarnings(List.of(total("Bostadsbidrag", "1250", "Bostadsbidrag")),
			Optional.of(Map.of()), EXACT);

		assertThat(warnings).containsExactly("Bostadsbidrag: ny inkomst sedan föregående normberäkning, 1250 kr – kontrollera summan");
	}

	@Test
	void aTypeThePreviousCalculationHadAtZeroIsANewIncomeNotADifference() {
		final var warnings = IncomeChangeFeeder.changeWarnings(List.of(total("Bostadsbidrag", "1250", "Bostadsbidrag")),
			previous("bostadsbidrag", "0"), EXACT);

		assertThat(warnings).containsExactly("Bostadsbidrag: ny inkomst sedan föregående normberäkning, 1250 kr – kontrollera summan");
	}

	@Test
	void nothingNewWhenTheCurrentAmountIsZeroToo() {
		assertThat(IncomeChangeFeeder.changeWarnings(List.of(total("Bostadsbidrag", "0", "Bostadsbidrag")), Optional.of(Map.of()), EXACT)).isEmpty();
	}

	@Test
	void noPreviousCalculationMeansNoComparison() {
		// A nyansökan: nothing was counted before, so nothing can have changed — and no threshold is asked.
		final List<IncomeTypeTotal> asked = new ArrayList<>();
		final Function<IncomeTypeTotal, IncomeChangeFeeder.Threshold> recording = total -> {
			asked.add(total);
			return IncomeChangeFeeder.Threshold.defaults();
		};

		assertThat(IncomeChangeFeeder.changeWarnings(List.of(total("Bostadsbidrag", "1250", "Bostadsbidrag")), Optional.empty(), recording)).isEmpty();
		assertThat(IncomeChangeFeeder.changeWarnings(List.of(total("Bostadsbidrag", "1250", "Bostadsbidrag")), null, recording)).isEmpty();
		assertThat(asked).isEmpty();
	}

	@Test
	void aTypeOnlyInThePreviousCalculationIsNotThisWarning() {
		// MISSING_SSBTEK already names it.
		assertThat(IncomeChangeFeeder.changeWarnings(List.of(), previous("bostadsbidrag", "1250"), EXACT)).isEmpty();
	}

	@Test
	void everyTextIsKeyedOnTheTypeName() {
		final var warnings = IncomeChangeFeeder.changeWarnings(List.of(total("Bostadsbidrag", "1300", "Bostadsbidrag"), total("Underhållsstöd", "1673", "Underhållsstöd")),
			previous("bostadsbidrag", "1250"), EXACT);

		// New or changed, the dedup key is the type — so a type keeps one warning as it moves between the two.
		assertThat(warnings).hasSize(2).extracting(WarningService::sourceKey).containsExactly("Bostadsbidrag", "Underhållsstöd");
	}

	@Test
	void incomeChangeWarningsAsksTheTableWithTheFirstBenefitInSortedOrder() {
		when(processServiceMock.evaluateDecision(MUNICIPALITY_ID, "Decision_inkomstTroskel", Map.of("forman", "Aktivitetsstöd")))
			.thenReturn(List.of(Map.of("troskelProcent", 0, "regel", "Dagersättning har ändrats")));

		final var warnings = feeder.incomeChangeWarnings(MUNICIPALITY_ID, List.of(total("Dagersättning från FK", "5010", "Dagersättning", "Aktivitetsstöd")),
			previous("dagersättning från fk", "5000"));

		assertThat(warnings).containsExactly("Dagersättning från FK: 5000 kr i föregående normberäkning → 5010 kr nu – Dagersättning har ändrats");
	}

	@Test
	void incomeChangeWarningsSkipsATypeTheTableSaysNotToCompare() {
		when(processServiceMock.evaluateDecision(MUNICIPALITY_ID, "Decision_inkomstTroskel", Map.of("forman", "Bostadsbidrag")))
			.thenReturn(List.of(Map.of("troskelProcent", -1L)));

		assertThat(feeder.incomeChangeWarnings(MUNICIPALITY_ID, List.of(total("Bostadsbidrag", "1250", "Bostadsbidrag")), Optional.of(Map.of()))).isEmpty();
	}

	@Test
	void incomeChangeWarningsFallsBackToTwelvePercentWhenTheTableCannotBeAsked() {
		when(processServiceMock.evaluateDecision(MUNICIPALITY_ID, "Decision_inkomstTroskel", Map.of("forman", "Bostadsbidrag")))
			.thenThrow(Problem.valueOf(BAD_GATEWAY, "engine down"));

		// 10 % is within the default tolerance; without the table's exact rule there is no warning and no text.
		assertThat(feeder.incomeChangeWarnings(MUNICIPALITY_ID, List.of(total("Bostadsbidrag", "1100", "Bostadsbidrag")), previous("bostadsbidrag", "1000"))).isEmpty();
		assertThat(feeder.incomeChangeWarnings(MUNICIPALITY_ID, List.of(total("Bostadsbidrag", "1200", "Bostadsbidrag")), previous("bostadsbidrag", "1000")))
			.containsExactly("Bostadsbidrag: 1000 kr i föregående normberäkning → 1200 kr nu");
	}

	@Test
	void incomeChangeWarningsFallsBackToTwelvePercentWhenTheTableAnswersNothing() {
		when(processServiceMock.evaluateDecision(MUNICIPALITY_ID, "Decision_inkomstTroskel", Map.of("forman", "Bostadsbidrag"))).thenReturn(List.of());

		assertThat(feeder.incomeChangeWarnings(MUNICIPALITY_ID, List.of(total("Bostadsbidrag", "1200", "Bostadsbidrag")), previous("bostadsbidrag", "1000")))
			.containsExactly("Bostadsbidrag: 1000 kr i föregående normberäkning → 1200 kr nu");
		verify(processServiceMock).evaluateDecision(MUNICIPALITY_ID, "Decision_inkomstTroskel", Map.of("forman", "Bostadsbidrag"));
	}

	@Test
	void incomeChangeWarningsUsesTheDefaultForATypeWithoutBenefitNames() {
		assertThat(feeder.incomeChangeWarnings(MUNICIPALITY_ID, List.of(total("Bostadsbidrag", "1200")), previous("bostadsbidrag", "1000")))
			.containsExactly("Bostadsbidrag: 1000 kr i föregående normberäkning → 1200 kr nu");
		verifyNoInteractions(processServiceMock);
	}

	@Test
	void incomeChangeWarningsDoesNotAskTheTableForAnUnchangedType() {
		assertThat(feeder.incomeChangeWarnings(MUNICIPALITY_ID, List.of(total("Bostadsbidrag", "1250", "Bostadsbidrag")), previous("bostadsbidrag", "1250"))).isEmpty();
		verifyNoInteractions(processServiceMock);
	}
}
