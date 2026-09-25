package se.sundsvall.caremanagement.types.financialassistance.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialAssistanceLabelsTest {

	@ParameterizedTest
	@CsvSource({
		"APPLICANT,Sökande",
		"CO_APPLICANT,Medsökande",
		"CHILD,Barn",
		"VISITATION_CHILD,Umgängesbarn"
	})
	void roleDisplayNameLabelsEveryRole(final String role, final String expected) {
		assertThat(FinancialAssistanceLabels.roleDisplayName(role)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({
		"NATIONAL_NORM,Riksnorm",
		"OTHER_NORM,Annan norm"
	})
	void normTypeDisplayNameLabelsEveryNormType(final String normType, final String expected) {
		assertThat(FinancialAssistanceLabels.normTypeDisplayName(normType)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({
		"SYSTEM,Automatiskt",
		"CASEWORKER,Handläggare"
	})
	void originDisplayNameLabelsEveryOrigin(final String origin, final String expected) {
		assertThat(FinancialAssistanceLabels.originDisplayName(origin)).isEqualTo(expected);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void anAbsentCodeHasNoLabelRatherThanBlowingUp(final String code) {
		// Map.of rejects a null key on get(), which is how a null role once reached a NullPointerException here.
		assertThat(FinancialAssistanceLabels.roleDisplayName(code)).isNull();
		assertThat(FinancialAssistanceLabels.normTypeDisplayName(code)).isNull();
		assertThat(FinancialAssistanceLabels.originDisplayName(code)).isNull();
		assertThat(FinancialAssistanceLabels.costDisplayName(code)).isNull();
		assertThat(FinancialAssistanceLabels.incomeDisplayName(code)).isNull();
	}

	@Test
	void anUnknownCodeKeepsItsOwnValue() {
		assertThat(FinancialAssistanceLabels.costDisplayName("NO_SUCH_COST")).isEqualTo("NO_SUCH_COST");
		assertThat(FinancialAssistanceLabels.incomeDisplayName("NO_SUCH_INCOME")).isEqualTo("NO_SUCH_INCOME");
		assertThat(FinancialAssistanceLabels.roleDisplayName("NO_SUCH_ROLE")).isNull();
	}

	@Test
	void costAndIncomeLabelsComeFromTheTypeCatalogue() {
		// The Lifecare (handläggare) label wins, so the warning text and the calculation row can never drift apart.
		assertThat(FinancialAssistanceLabels.costDisplayName("RENT")).isEqualTo("Boendekostnad");
		assertThat(FinancialAssistanceLabels.costDisplayName("DENTAL_CARE")).isEqualTo("Tandvård");
		assertThat(FinancialAssistanceLabels.incomeDisplayName("HOUSING_ALLOWANCE")).isEqualTo("Bostadsbidrag");
	}

	@Test
	void theApplicationsIncomeLabelIsTheOneTheApplicantChose() {
		assertThat(FinancialAssistanceLabels.applicationIncomeDisplayName("SWISH_DEPOSITS")).isEqualTo("Swish/kontoinsättningar");
		assertThat(FinancialAssistanceLabels.applicationIncomeDisplayName("HOUSING_ALLOWANCE")).isEqualTo("Bostadsbidrag");
		assertThat(FinancialAssistanceLabels.applicationIncomeDisplayName("NO_SUCH_INCOME")).isEqualTo("NO_SUCH_INCOME");
		assertThat(FinancialAssistanceLabels.applicationIncomeDisplayName(" ")).isNull();
		assertThat(FinancialAssistanceLabels.applicationIncomeDisplayName(null)).isNull();
	}

	@Test
	void aTypeWithOnlyACitizenLabelFallsBackToIt() {
		assertThat(FinancialAssistanceLabels.incomeDisplayName("FINANCIAL_AID_OTHER_MUNICIPALITY")).isEqualTo("Ekonomiskt bistånd från annan kommun");
	}
}
