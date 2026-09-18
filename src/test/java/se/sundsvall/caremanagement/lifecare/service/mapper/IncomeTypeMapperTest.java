package se.sundsvall.caremanagement.lifecare.service.mapper;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
			// names verified against the live FamilyCare Calculations/Proposals catalogue (2026-09-18)
			Arguments.of("Lön efter skatt", "SALARY"),
			Arguments.of("  LÖN EFTER SKATT  ", "SALARY"),
			Arguments.of("Pension/SA/Livränta/Omvårdnadsbidrag", "OCCUPATIONAL_PENSION_INSURANCE"),
			Arguments.of("Underhållsstöd", "CHILD_SUPPORT"),
			Arguments.of("Swish/Insättningar/Överföringar", "SWISH_DEPOSITS"),
			// "Övriga inkomster" is posted to by hyresdel, annan inkomst and bistånd från annan kommun alike, so a
			// previous calculation's row cannot be attributed to any one of them
			Arguments.of("Övriga inkomster", null),
			// guessed names that do not exist in the catalogue — these are what the old fragment table matched on
			Arguments.of("PLV", null),
			Arguments.of("Tjänstepension", null),
			Arguments.of("Underhållsbidrag från den andra föräldern", null),
			Arguments.of("Hyresdel från barn", null),
			// real catalogue names that must not be mistaken for a compared type
			Arguments.of("Barnpension", null),
			Arguments.of("Pension", null),
			Arguments.of("Bostadsbidrag", null),
			Arguments.of("Sjukersättning", null),
			Arguments.of("Aktivitetsersättning", null));
	}

	@Test
	void everyUnambiguousForwardNameResolvesBack() {
		final var forward = ApplicationIncomeToFamilyCareMapper.APPLICATION_TYPE_TO_FC_NAME;

		forward.forEach((incomeType, familyCareName) -> {
			final var sharedName = forward.values().stream().filter(familyCareName::equals).count() > 1;
			if (!sharedName) {
				assertThat(IncomeTypeMapper.incomeTypeForFamilyCareName(familyCareName)).contains(incomeType);
			} else {
				assertThat(IncomeTypeMapper.incomeTypeForFamilyCareName(familyCareName)).isEmpty();
			}
		});
	}

	@Test
	void sharedFamilyCareNamesAreDroppedFromTheTable() {
		// "Övriga inkomster" is claimed by three income types; resolving it to one of them would attribute a previous
		// amount to the wrong type and fire a false "belopp skiljer sig" warning.
		assertThat(IncomeTypeMapper.incomeTypeByName()).doesNotContainKey("övriga inkomster");
		assertThat(IncomeTypeMapper.incomeTypeByName()).containsValues("SALARY", "OCCUPATIONAL_PENSION_INSURANCE", "CHILD_SUPPORT");
		assertThat(IncomeTypeMapper.incomeTypeByName()).doesNotContainValue("RENT_SHARE_FROM_CHILD");
	}

	@Test
	void tableIsUnmodifiable() {
		assertThatThrownBy(() -> IncomeTypeMapper.incomeTypeByName().put("x", "Y"))
			.isInstanceOf(UnsupportedOperationException.class);
	}
}
