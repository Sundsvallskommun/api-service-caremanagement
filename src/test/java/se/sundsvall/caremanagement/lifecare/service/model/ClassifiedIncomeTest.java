package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ClassifiedIncomeTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * The {@code classifiedIncomes} JSON the operaton regelverk worker produces keeps Swedish keys ({@code atgard},
	 * {@code varning}, {@code regel}) — they must deserialise onto the English record components via {@link
	 * com.fasterxml.jackson.annotation.JsonProperty}. This exercises the real parse (which the service tests mock out).
	 */
	@Test
	void deserialisesTheSwedishKeyedRegelverkJson() {
		final var json = """
			{
			  "income": {
			    "benefit": "Bostadsbidrag",
			    "subBenefit": null,
			    "amountType": "Månad",
			    "netAmount": 1850,
			    "period": "2026-05-15",
			    "role": "APPLICANT"
			  },
			  "atgard": "TA_MED_KVITTNING",
			  "calculation": "Bostadsbidrag",
			  "varning": true,
			  "regel": "Ta med kvittning"
			}
			""";

		final var result = objectMapper.readValue(json, ClassifiedIncome.class);

		assertThat(result.action()).isEqualTo("TA_MED_KVITTNING");
		assertThat(result.warning()).isTrue();
		assertThat(result.rule()).isEqualTo("Ta med kvittning");
		assertThat(result.calculation()).isEqualTo("Bostadsbidrag");
		assertThat(result.income()).isNotNull();
		assertThat(result.income().benefit()).isEqualTo("Bostadsbidrag");
		assertThat(result.income().netAmount()).isEqualByComparingTo(new BigDecimal("1850"));
		assertThat(result.income().period()).isEqualTo(LocalDate.of(2026, 5, 15));
		assertThat(result.income().role()).isEqualTo(ApplicantRole.APPLICANT);
	}
}
