package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityResponseTest {

	private static final List<ApplicationSuggestion> SUGGESTIONS = List.of(
		ApplicationSuggestion.create().withTypeSlug("financial-assistance-renewal").withRecommended(true));

	@Test
	void builderMethods() {
		final var response = EligibilityResponse.create()
			.withSuggestions(SUGGESTIONS)
			.withRequiresCaseworker(true)
			.withReasonCode("DECISION_FOR_CURRENT_MONTH")
			.withMessage("Beslut finns")
			.withHasRecentApplication(true)
			.withWindowDays(90)
			.withHasOpenCase(true)
			.withHasDecisionForCurrentMonth(true)
			.withLatestDecisionPeriodMonth(5)
			.withLatestDecisionPeriodYear(2026)
			.withHasPreviousCalculation(true)
			.withConstellationMatchesPrevious(true)
			.withLifecareChecked(true)
			.withHasCoApplicant(true);

		assertThat(response.getSuggestions()).isEqualTo(SUGGESTIONS);
		assertThat(response.isRequiresCaseworker()).isTrue();
		assertThat(response.getReasonCode()).isEqualTo("DECISION_FOR_CURRENT_MONTH");
		assertThat(response.getMessage()).isEqualTo("Beslut finns");
		assertThat(response.isHasRecentApplication()).isTrue();
		assertThat(response.getWindowDays()).isEqualTo(90);
		assertThat(response.isHasOpenCase()).isTrue();
		assertThat(response.isHasDecisionForCurrentMonth()).isTrue();
		assertThat(response.getLatestDecisionPeriodMonth()).isEqualTo(5);
		assertThat(response.getLatestDecisionPeriodYear()).isEqualTo(2026);
		assertThat(response.isHasPreviousCalculation()).isTrue();
		assertThat(response.getConstellationMatchesPrevious()).isTrue();
		assertThat(response.isLifecareChecked()).isTrue();
		assertThat(response.isHasCoApplicant()).isTrue();
		assertThat(response).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersWork() {
		final var response = EligibilityResponse.create();
		response.setSuggestions(SUGGESTIONS);
		response.setRequiresCaseworker(false);
		response.setReasonCode("NO_OPEN_CASE");
		response.setMessage("Föreslår nyansökan");
		response.setHasRecentApplication(false);
		response.setWindowDays(30);
		response.setHasOpenCase(false);
		response.setHasDecisionForCurrentMonth(false);
		response.setLatestDecisionPeriodMonth(null);
		response.setLatestDecisionPeriodYear(null);
		response.setHasPreviousCalculation(false);
		response.setConstellationMatchesPrevious(null);
		response.setLifecareChecked(false);
		response.setHasCoApplicant(false);

		assertThat(response.getReasonCode()).isEqualTo("NO_OPEN_CASE");
		assertThat(response.getMessage()).isEqualTo("Föreslår nyansökan");
		assertThat(response.getWindowDays()).isEqualTo(30);
		assertThat(response.isLifecareChecked()).isFalse();
	}

	@Test
	void equalsAndHashCode() {
		final var a = EligibilityResponse.create().withReasonCode("NO_OPEN_CASE").withWindowDays(90).withSuggestions(SUGGESTIONS);
		final var b = EligibilityResponse.create().withReasonCode("NO_OPEN_CASE").withWindowDays(90).withSuggestions(SUGGESTIONS);
		final var c = EligibilityResponse.create().withReasonCode("RECENT_APPLICATION");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
	}
}
