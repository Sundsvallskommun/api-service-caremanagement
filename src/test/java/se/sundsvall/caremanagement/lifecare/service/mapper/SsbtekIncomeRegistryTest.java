package se.sundsvall.caremanagement.lifecare.service.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SsbtekIncomeRegistryTest {

	@ParameterizedTest
	@CsvSource({
		"Dagersättning,Dagersättning FK",
		"Bostadsbidrag,Bostadsbidrag",
		"bostadsbidrag,Bostadsbidrag",            // case-insensitive
		"'  Bostadsbidrag  ',Bostadsbidrag",      // trims surrounding whitespace
		"Pension/SA/Livräntor/Vårdbidrag,PLV",
		"Allmänt barnbidrag,Barnbidrag",
		"Underhållsstöd,Underhållsstöd",
		"Studiemedel,Studiemedel",
		"Studiehjalp,Studiebidrag (gymn)",
		"Arbetslöshetsersättning,A-kassa/Alfa",
		"PM-Prel,PM-PREL",
		"PM,PM",
		"Elstöd,Elstöd",
		"Skattekontouppgift,Skatteåterbäring"
	})
	void fcNormberakningTypeForIncluded(final String forman, final String expected) {
		assertThat(SsbtekIncomeRegistry.fcNormberakningType(forman)).contains(expected);
		assertThat(SsbtekIncomeRegistry.isExcluded(forman)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"Handikappersättning",
		"Bostadskostnad"
	})
	void excludedFormenAreNotTransferredButAreKnown(final String forman) {
		assertThat(SsbtekIncomeRegistry.fcNormberakningType(forman)).isEmpty();
		assertThat(SsbtekIncomeRegistry.isExcluded(forman)).isTrue();
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"Försörjningsstöd",
		"",
		"   "
	})
	void unknownFormenAreEmptyAndNotExcluded(final String forman) {
		assertThat(SsbtekIncomeRegistry.fcNormberakningType(forman)).isEmpty();
		assertThat(SsbtekIncomeRegistry.isExcluded(forman)).isFalse();
	}

	@Test
	void normalizeIsNullSafe() {
		assertThat(SsbtekIncomeRegistry.normalize(null)).isEmpty();
		assertThat(SsbtekIncomeRegistry.normalize("  Mixed Case  ")).isEqualTo("mixed case");
	}
}
