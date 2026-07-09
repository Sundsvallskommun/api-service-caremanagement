package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.time.OffsetDateTime;
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
			.withReasonCode("EXISTING_CASE")
			.withMessage("Befintligt errand")
			.withExistsInCm(true)
			.withExistsInLc(true)
			.withMaritalStatusMatches(true)
			.withWindowDays(90)
			.withApplicationExistsThisMonth(true)
			.withApplicationExistsNextMonth(false)
			.withCurrentMonthDecided(true)
			.withLatestDecisionPeriodMonth(5)
			.withLatestDecisionPeriodYear(2026)
			.withHasPreviousCalculation(true)
			.withLifecareChecked(true)
			.withHasCoApplicant(true)
			.withReopenableErrandId("errand-9")
			.withClosedAt(OffsetDateTime.parse("2026-06-20T10:15:30Z"));

		assertThat(response.getSuggestions()).isEqualTo(SUGGESTIONS);
		assertThat(response.getReasonCode()).isEqualTo("EXISTING_CASE");
		assertThat(response.getMessage()).isEqualTo("Befintligt errand");
		assertThat(response.isExistsInCm()).isTrue();
		assertThat(response.isExistsInLc()).isTrue();
		assertThat(response.getMaritalStatusMatches()).isTrue();
		assertThat(response.getWindowDays()).isEqualTo(90);
		assertThat(response.isApplicationExistsThisMonth()).isTrue();
		assertThat(response.isApplicationExistsNextMonth()).isFalse();
		assertThat(response.isCurrentMonthDecided()).isTrue();
		assertThat(response.getLatestDecisionPeriodMonth()).isEqualTo(5);
		assertThat(response.getLatestDecisionPeriodYear()).isEqualTo(2026);
		assertThat(response.isHasPreviousCalculation()).isTrue();
		assertThat(response.isLifecareChecked()).isTrue();
		assertThat(response.isHasCoApplicant()).isTrue();
		assertThat(response.getReopenableErrandId()).isEqualTo("errand-9");
		assertThat(response.getClosedAt()).isEqualTo(OffsetDateTime.parse("2026-06-20T10:15:30Z"));
		assertThat(response).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersWork() {
		final var response = EligibilityResponse.create();
		response.setSuggestions(SUGGESTIONS);
		response.setReasonCode("NO_EXISTING_CASE");
		response.setMessage("Föreslår nyansökan");
		response.setExistsInCm(false);
		response.setExistsInLc(false);
		response.setMaritalStatusMatches(null);
		response.setWindowDays(30);
		response.setApplicationExistsThisMonth(false);
		response.setApplicationExistsNextMonth(false);
		response.setCurrentMonthDecided(false);
		response.setLatestDecisionPeriodMonth(null);
		response.setLatestDecisionPeriodYear(null);
		response.setHasPreviousCalculation(false);
		response.setLifecareChecked(false);
		response.setHasCoApplicant(false);

		assertThat(response.getReasonCode()).isEqualTo("NO_EXISTING_CASE");
		assertThat(response.getMessage()).isEqualTo("Föreslår nyansökan");
		assertThat(response.getWindowDays()).isEqualTo(30);
		assertThat(response.getMaritalStatusMatches()).isNull();
		assertThat(response.isLifecareChecked()).isFalse();
	}

	@Test
	void equalsAndHashCode() {
		final var a = EligibilityResponse.create().withReasonCode("NO_EXISTING_CASE").withWindowDays(90).withSuggestions(SUGGESTIONS);
		final var b = EligibilityResponse.create().withReasonCode("NO_EXISTING_CASE").withWindowDays(90).withSuggestions(SUGGESTIONS);
		final var c = EligibilityResponse.create().withReasonCode("MARITAL_STATUS_CHANGED");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}
}
