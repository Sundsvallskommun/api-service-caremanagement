package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.Month.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class NormPersonInputTest {

	@Test
	void testBean() {
		assertThat(NormPersonInput.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var partyId = "partyId";
		final var role = "APPLICANT";
		final var name = "name";
		final var caseworkerDays = 15;
		final var included = true;
		final var deviationFromDate = LocalDate.of(2026, JUNE, 1);
		final var deviationToDate = LocalDate.of(2026, JUNE, 15);
		final var normInterval = "MONTH";
		final var jobStimulusAmount = BigDecimal.valueOf(1000.00);
		final var note = "note";

		final var result = NormPersonInput.create()
			.withPartyId(partyId)
			.withRole(role)
			.withName(name)
			.withCaseworkerDays(caseworkerDays)
			.withIncluded(included)
			.withDeviationFromDate(deviationFromDate)
			.withDeviationToDate(deviationToDate)
			.withNormInterval(normInterval)
			.withJobStimulusAmount(jobStimulusAmount)
			.withNote(note);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getPartyId()).isEqualTo(partyId);
		assertThat(result.getRole()).isEqualTo(role);
		assertThat(result.getName()).isEqualTo(name);
		assertThat(result.getCaseworkerDays()).isEqualTo(caseworkerDays);
		assertThat(result.getIncluded()).isEqualTo(included);
		assertThat(result.getDeviationFromDate()).isEqualTo(deviationFromDate);
		assertThat(result.getDeviationToDate()).isEqualTo(deviationToDate);
		assertThat(result.getNormInterval()).isEqualTo(normInterval);
		assertThat(result.getJobStimulusAmount()).isEqualTo(jobStimulusAmount);
		assertThat(result.getNote()).isEqualTo(note);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormPersonInput.create()).hasAllNullFieldsOrProperties();
		assertThat(new NormPersonInput()).hasAllNullFieldsOrProperties();
	}
}
