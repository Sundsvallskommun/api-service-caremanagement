package se.sundsvall.caremanagement.lifecare.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.APPLICANT;

class SsbtekPeriodSelectorTest {

	private static final YearMonth MAY_2026 = YearMonth.of(2026, 5); // kontroll = April, jämförelse = March

	private static SsbtekIncome income(final String forman, final String date) {
		return new SsbtekIncome(forman, null, null, BigDecimal.ONE, date == null ? null : LocalDate.parse(date), APPLICANT);
	}

	@Test
	void transfersKontrollperiodIncomes() {
		final var selected = SsbtekPeriodSelector.selectTransferable(List.of(income("Bostadsbidrag", "2026-04-10")), MAY_2026);

		assertThat(selected).extracting(SsbtekIncome::forman).containsExactly("Bostadsbidrag");
	}

	@Test
	void transfersJamforelseperiodIncomeWhenFormanNotInKontroll() {
		final var incomes = List.of(
			income("Bostadsbidrag", "2026-04-10"),  // kontroll
			income("Barnbidrag", "2026-03-10"));    // jämförelse, different förmån

		final var selected = SsbtekPeriodSelector.selectTransferable(incomes, MAY_2026);

		assertThat(selected).extracting(SsbtekIncome::forman).containsExactlyInAnyOrder("Bostadsbidrag", "Barnbidrag");
	}

	@Test
	void dedupesJamforelseperiodIncomeAlreadyPresentInKontroll() {
		final var incomes = List.of(
			income("Bostadsbidrag", "2026-04-10"),  // kontroll
			income("Bostadsbidrag", "2026-03-10")); // jämförelse, same förmån -> already covered

		final var selected = SsbtekPeriodSelector.selectTransferable(incomes, MAY_2026);

		assertThat(selected)
			.extracting(SsbtekIncome::period)
			.containsExactly(LocalDate.parse("2026-04-10"));
	}

	@Test
	void dedupIsCaseInsensitive() {
		final var incomes = List.of(
			income("Bostadsbidrag", "2026-04-10"),  // kontroll
			income("bostadsbidrag", "2026-03-10")); // jämförelse, same förmån different case

		final var selected = SsbtekPeriodSelector.selectTransferable(incomes, MAY_2026);

		assertThat(selected).hasSize(1);
		assertThat(selected.getFirst().period()).isEqualTo(LocalDate.parse("2026-04-10"));
	}

	@Test
	void ignoresAnsokningsperiodOutsideAndUndatedIncomes() {
		final var incomes = Arrays.asList(
			income("Lön", "2026-05-10"),        // ansökningsperiod
			income("Lön", "2026-01-10"),        // outside all three periods
			income("Lön", null),                // undated
			null);                              // null entry

		final var selected = SsbtekPeriodSelector.selectTransferable(incomes, MAY_2026);

		assertThat(selected).isEmpty();
	}

	@Test
	void nullIncomesReturnsEmpty() {
		assertThat(SsbtekPeriodSelector.selectTransferable(null, MAY_2026)).isEmpty();
	}
}
