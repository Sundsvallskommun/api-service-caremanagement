package se.sundsvall.caremanagement.lifecare.service.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static java.time.Month.MAY;
import static org.assertj.core.api.Assertions.assertThat;

class ClassifiedIncomeTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * The {@code classifiedIncomes} JSON the operaton regelverk worker produces keeps Swedish keys throughout — both the
	 * outer verdict ({@code atgard}, {@code normberakning}, {@code varning}, {@code regel}) and the nested {@code income}
	 * object ({@code forman}, {@code delforman}, {@code beloppstyp}; {@code netAmount}/{@code period}/ {@code role}
	 * already share the wire key). They must deserialise onto the English record components via {@link
	 * com.fasterxml.jackson.annotation.JsonProperty}. This exercises the real parse (which the service tests mock out)
	 * with the exact keys operaton serialises, so a missing/wrong {@code JsonProperty} — which nulls the field silently
	 * — can't slip through: {@code normberakning} → {@link ClassifiedIncome#calculation()} drives the FamilyCare
	 * income-type lookup, and {@code forman}/{@code delforman}/{@code beloppstyp} → the {@link SsbtekIncome} descriptor.
	 */
	@Test
	void deserialisesTheSwedishKeyedRegelverkJson() {
		final var json = """
			{
			  "income": {
			    "forman": "Bostadsbidrag",
			    "delforman": "Barnfamilj",
			    "beloppstyp": "Månad",
			    "netAmount": 1850,
			    "period": "2026-05-15",
			    "role": "APPLICANT"
			  },
			  "atgard": "TA_MED_KVITTNING",
			  "normberakning": "Bostadsbidrag",
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
		assertThat(result.income().subBenefit()).isEqualTo("Barnfamilj");
		assertThat(result.income().amountType()).isEqualTo("Månad");
		assertThat(result.income().netAmount()).isEqualByComparingTo(new BigDecimal("1850"));
		assertThat(result.income().period()).isEqualTo(LocalDate.of(2026, MAY, 15));
		assertThat(result.income().role()).isEqualTo(ApplicantRole.APPLICANT);
	}
}
