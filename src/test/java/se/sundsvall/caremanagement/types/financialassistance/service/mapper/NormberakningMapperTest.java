package se.sundsvall.caremanagement.types.financialassistance.service.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.lifecare.service.model.NormberakningResult;
import se.sundsvall.caremanagement.lifecare.service.model.SsbtekChangeWarning;
import se.sundsvall.caremanagement.lifecare.service.model.UnhandledIncome;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.caremanagement.lifecare.service.model.UnhandledReason.NOT_ON_WHITELIST;

class NormberakningMapperTest {

	@Test
	void flattensResultToResponse() {
		final var result = new NormberakningResult(4711,
			List.of(new UnhandledIncome("Bostadstillägg", "del", "Månad", NOT_ON_WHITELIST)),
			List.of(new SsbtekChangeWarning("Bostadsbidrag", new BigDecimal("2400"), new BigDecimal("1850"), new BigDecimal("-23"))));

		final var response = NormberakningMapper.toResponse(result);

		assertThat(response.getCalculationId()).isEqualTo(4711);
		assertThat(response.getUnhandledIncomes()).containsExactly("Bostadstillägg / del / Månad (NOT_ON_WHITELIST)");
		assertThat(response.getChangeWarnings()).containsExactly("Bostadsbidrag: -23% (jämförelse 2400 → kontroll 1850)");
	}

	@Test
	void nullResultGivesEmptyResponse() {
		final var response = NormberakningMapper.toResponse(null);

		assertThat(response.getCalculationId()).isNull();
		assertThat(response.getUnhandledIncomes()).isEmpty();
		assertThat(response.getChangeWarnings()).isEmpty();
	}
}
