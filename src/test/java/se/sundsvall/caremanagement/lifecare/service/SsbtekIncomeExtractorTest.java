package se.sundsvall.caremanagement.lifecare.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekIncome;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.APPLICANT;
import static se.sundsvall.caremanagement.lifecare.service.model.ApplicantRole.CO_APPLICANT;

class SsbtekIncomeExtractorTest {

	/** Mirrors the real financial-aid SO fixture shape (one arbetslöshetsersättning with one Utbetalning). */
	private static Map<String, Object> soBasis(final Object utbetalningar) {
		return Map.of("so", Map.of(
			"ArbetsloshetsersattningLista", Map.of(
				"Arbetsloshetsersattning", Map.of(
					"Utbetalningar", utbetalningar))));
	}

	private static Map<String, Object> payment(final String netto, final String datum) {
		return Map.of("NettoEfterSkatt", netto, "Utbetalningsdatum", datum);
	}

	@Test
	void extractsSingleArbetsloshetsersattningPayment() {
		final var incomes = SsbtekIncomeExtractor.extract(soBasis(payment("1250.50", "2013-11-23")), APPLICANT);

		assertThat(incomes).singleElement().isEqualTo(
			new SsbtekIncome("Arbetslöshetsersättning", null, null, new BigDecimal("1250.50"), LocalDate.parse("2013-11-23"), APPLICANT));
	}

	@Test
	void honoursTheSuppliedRole() {
		final var incomes = SsbtekIncomeExtractor.extract(soBasis(payment("900", "2026-04-10")), CO_APPLICANT);

		assertThat(incomes).singleElement().satisfies(income -> assertThat(income.role()).isEqualTo(CO_APPLICANT));
	}

	@Test
	void handlesMultiplePaymentsAsAList() {
		final var incomes = SsbtekIncomeExtractor.extract(
			soBasis(List.of(payment("1000", "2026-04-10"), payment("250", "2026-04-20"))), APPLICANT);

		assertThat(incomes)
			.extracting(SsbtekIncome::netAmount)
			.containsExactly(new BigDecimal("1000"), new BigDecimal("250"));
	}

	@Test
	void skipsPaymentsWithoutAnAmount() {
		final var incomes = SsbtekIncomeExtractor.extract(
			soBasis(Map.of("Utbetalningsdatum", "2026-04-10")), APPLICANT);

		assertThat(incomes).isEmpty();
	}

	@Test
	void toleratesUnparseableAmountAndDate() {
		final var incomes = SsbtekIncomeExtractor.extract(
			soBasis(payment("not-a-number", "nope")), APPLICANT);

		assertThat(incomes).isEmpty();
	}

	@Test
	void keepsIncomeWithUnparseableDateButValidAmount() {
		final var incomes = SsbtekIncomeExtractor.extract(soBasis(payment("500", "garbled")), APPLICANT);

		assertThat(incomes).singleElement().satisfies(income -> {
			assertThat(income.netAmount()).isEqualTo(new BigDecimal("500"));
			assertThat(income.period()).isNull();
		});
	}

	@Test
	void toleratesFullLengthButInvalidDate() {
		// 10+ chars (passes the length guard) but not a real calendar date — must fall back to null, not throw.
		final var incomes = SsbtekIncomeExtractor.extract(soBasis(payment("500", "2026-13-45")), APPLICANT);

		assertThat(incomes).singleElement().satisfies(income -> assertThat(income.period()).isNull());
	}

	@Test
	void parsesDateWithTrailingTimeOrOffset() {
		final var incomes = SsbtekIncomeExtractor.extract(soBasis(payment("500", "1998-05-12+02:00")), APPLICANT);

		assertThat(incomes).singleElement().satisfies(income -> assertThat(income.period()).isEqualTo(LocalDate.parse("1998-05-12")));
	}

	@Test
	void returnsEmptyForNullOrEmptyOrNonIncomeAgencies() {
		assertThat(SsbtekIncomeExtractor.extract(null, APPLICANT)).isEmpty();
		assertThat(SsbtekIncomeExtractor.extract(Map.of(), APPLICANT)).isEmpty();
		// af/tns/miv carry status/assets/permits, not income — and fk/csn are not yet grounded.
		assertThat(SsbtekIncomeExtractor.extract(Map.of("af", Map.of("ArbetssokandeInfo", Map.of("Arbetssokande", "true")), "fk", Map.of(), "miv", Map.of()), APPLICANT)).isEmpty();
	}

	@Test
	void toleratesMalformedSoStructure() {
		assertThat(SsbtekIncomeExtractor.extract(Map.of("so", "unexpected-string"), APPLICANT)).isEmpty();
		assertThat(SsbtekIncomeExtractor.extract(Map.of("so", Map.of("ArbetsloshetsersattningLista", "x")), APPLICANT)).isEmpty();
	}
}
