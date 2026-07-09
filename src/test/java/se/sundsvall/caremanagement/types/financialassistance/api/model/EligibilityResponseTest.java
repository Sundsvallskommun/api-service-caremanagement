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
	void testSettersWork() {
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
	void testNoDirtOnCreatedBean() {
		assertThat(EligibilityResponse.create()).hasAllNullFieldsOrPropertiesExcept(
			"existsInCm", "existsInLc", "windowDays", "applicationExistsThisMonth", "applicationExistsNextMonth", "currentMonthDecided",
			"hasPreviousCalculation", "lifecareChecked", "hasCoApplicant");
		assertThat(new EligibilityResponse()).hasAllNullFieldsOrPropertiesExcept(
			"existsInCm", "existsInLc", "windowDays", "applicationExistsThisMonth", "applicationExistsNextMonth", "currentMonthDecided",
			"hasPreviousCalculation", "lifecareChecked", "hasCoApplicant");
	}

	@Test
	void testEqualsAndHashCode() {
		final var a = EligibilityResponse.create().withReasonCode("NO_EXISTING_CASE").withWindowDays(90).withSuggestions(SUGGESTIONS);
		final var b = EligibilityResponse.create().withReasonCode("NO_EXISTING_CASE").withWindowDays(90).withSuggestions(SUGGESTIONS);
		final var c = EligibilityResponse.create().withReasonCode("MARITAL_STATUS_CHANGED");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}
}
