package se.sundsvall.caremanagement.types.financialassistance.service;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDateRuleTest {

	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource({
		"2026-05, 2026-05-27", // Wednesday → stays
		"2026-06, 2026-06-26", // 27th is a Saturday → Friday the 26th
		"2026-09, 2026-09-25", // 27th is a Sunday → Friday the 25th
		"2026-11, 2026-11-27", // Friday → stays
		"2026-07, 2026-07-27" // Monday → stays
	})
	void paymentDateMovesWeekendsToTheFridayBefore(final String month, final String expected) {
		assertThat(PaymentDateRule.paymentDate(YearMonth.parse(month))).isEqualTo(LocalDate.parse(expected));
	}
}
