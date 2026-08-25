package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class EligibilityResponseTest {

	private static final List<ApplicationSuggestion> SUGGESTIONS = List.of(
		ApplicationSuggestion.create().withTypeSlug("financial-assistance-renewal").withRecommended(true));

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(EligibilityResponse.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var response = EligibilityResponse.create()
			.withSuggestions(SUGGESTIONS)
			.withReasonCode("EXISTING_CASE")
			.withMessage("Befintligt errand")
			.withIntroText("Utifrån dina uppgifter kan du göra någon av följande ansökningar:")
			.withExistsInCm(true)
			.withExistsInLc(true)
			.withHasOpenCase(true)
			.withMaritalStatusMatches(true)
			.withWindowDays(90)
			.withApplicationExistsThisMonth(true)
			.withApplicationExistsNextMonth(false)
			.withCurrentMonthDecided(true)
			.withPreviousMonthDecided(true)
			.withMonthBeforePreviousDecided(false)
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
		assertThat(response.getIntroText()).isEqualTo("Utifrån dina uppgifter kan du göra någon av följande ansökningar:");
		assertThat(response.isExistsInCm()).isTrue();
		assertThat(response.isExistsInLc()).isTrue();
		assertThat(response.getHasOpenCase()).isTrue();
		assertThat(response.getMaritalStatusMatches()).isTrue();
		assertThat(response.getWindowDays()).isEqualTo(90);
		assertThat(response.isApplicationExistsThisMonth()).isTrue();
		assertThat(response.isApplicationExistsNextMonth()).isFalse();
		assertThat(response.isCurrentMonthDecided()).isTrue();
		assertThat(response.isPreviousMonthDecided()).isTrue();
		assertThat(response.isMonthBeforePreviousDecided()).isFalse();
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
	void testNoDirtOnCreatedBean() {
		assertThat(EligibilityResponse.create()).hasAllNullFieldsOrPropertiesExcept(
			"existsInCm", "existsInLc", "windowDays", "applicationExistsThisMonth", "applicationExistsNextMonth", "currentMonthDecided",
			"previousMonthDecided", "monthBeforePreviousDecided", "hasPreviousCalculation", "lifecareChecked", "hasCoApplicant");
		assertThat(new EligibilityResponse()).hasAllNullFieldsOrPropertiesExcept(
			"existsInCm", "existsInLc", "windowDays", "applicationExistsThisMonth", "applicationExistsNextMonth", "currentMonthDecided",
			"previousMonthDecided", "monthBeforePreviousDecided", "hasPreviousCalculation", "lifecareChecked", "hasCoApplicant");
	}

}
