package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
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
		final var handlaggareDays = 15;
		final var note = "note";

		final var result = NormPersonInput.create()
			.withPartyId(partyId)
			.withRole(role)
			.withName(name)
			.withHandlaggareDays(handlaggareDays)
			.withNote(note);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getPartyId()).isEqualTo(partyId);
		assertThat(result.getRole()).isEqualTo(role);
		assertThat(result.getName()).isEqualTo(name);
		assertThat(result.getHandlaggareDays()).isEqualTo(handlaggareDays);
		assertThat(result.getNote()).isEqualTo(note);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NormPersonInput.create()).hasAllNullFieldsOrProperties();
		assertThat(new NormPersonInput()).hasAllNullFieldsOrProperties();
	}
}
