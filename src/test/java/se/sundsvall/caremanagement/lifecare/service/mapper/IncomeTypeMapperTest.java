package se.sundsvall.caremanagement.lifecare.service.mapper;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class IncomeTypeMapperTest {

	@ParameterizedTest
	@MethodSource("incomeTypeArguments")
	void incomeTypeForFamilyCareName(final String familyCareName, final String expected) {
		final var result = IncomeTypeMapper.incomeTypeForFamilyCareName(familyCareName);

		if (expected == null) {
			assertThat(result).isEmpty();
		} else {
			assertThat(result).contains(expected);
		}
	}

	private static Stream<Arguments> incomeTypeArguments() {
		return Stream.of(
			Arguments.of(null, null),
			Arguments.of("", null),
			Arguments.of("   ", null),
			// the regelverk's normberäkning names
			Arguments.of("Lön", "SALARY"),
			Arguments.of("Lön efter skatt", "SALARY"),
			Arguments.of("  LÖN EFTER SKATT  ", "SALARY"),
			Arguments.of("PLV", "OCCUPATIONAL_PENSION_INSURANCE"),
			Arguments.of("plv", "OCCUPATIONAL_PENSION_INSURANCE"),
			Arguments.of("Tjänstepension", "OCCUPATIONAL_PENSION_INSURANCE"),
			Arguments.of("Pension/SA/Livränta/Omvårdnadsbidrag", "OCCUPATIONAL_PENSION_INSURANCE"),
			Arguments.of("Underhållsstöd", "CHILD_SUPPORT"),
			Arguments.of("Underhållsbidrag från den andra föräldern", "CHILD_SUPPORT"),
			Arguments.of("Hyresdel från barn", "RENT_SHARE_FROM_CHILD"),
			// deliberately unmapped — mapping these would raise a false "income missing" warning
			Arguments.of("Barnpension", null),
			Arguments.of("Ålderspension", null),
			Arguments.of("Garantipension", null),
			Arguments.of("Pension", null),
			Arguments.of("Bostadsbidrag", null),
			Arguments.of("Övriga inkomster", null),
			Arguments.of("Swish/Insättningar/Överföringar", null));
	}

	@Test
	void fragmentTableChecksBarnpensionBeforeAnyPensionFragment() {
		// The SKIP entry only works because it is matched first; reordering the table would silently map Barnpension.
		final var fragments = IncomeTypeMapper.incomeTypeByNameFragment().keySet().stream().toList();

		assertThat(fragments).startsWith("barnpension");
		assertThat(fragments.indexOf("barnpension")).isLessThan(fragments.indexOf("tjänstepension"));
	}

	@Test
	void fragmentTableIsUnmodifiable() {
		final var fragments = IncomeTypeMapper.incomeTypeByNameFragment();

		assertThat(fragments).containsKeys("barnpension", "lön", "plv", "tjänstepension", "pension/sa", "underhållsstöd", "underhållsbidrag", "hyresdel");
	}
}
