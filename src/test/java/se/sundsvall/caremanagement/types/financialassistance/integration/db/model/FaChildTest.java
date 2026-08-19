package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FaChildTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(FaChild.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var partyId = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
		final var firstName = "Anna";
		final var lastName = "Andersson";
		final var schoolName = "Sundsvalls skola";
		final var residenceExtent = "FULL_TIME";
		final var daysInHome = 14;

		final var result = FaChild.create()
			.withPartyId(partyId)
			.withFirstName(firstName)
			.withLastName(lastName)
			.withSchoolName(schoolName)
			.withResidenceExtent(residenceExtent)
			.withDaysInHome(daysInHome);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getPartyId()).isEqualTo(partyId);
		assertThat(result.getFirstName()).isEqualTo(firstName);
		assertThat(result.getLastName()).isEqualTo(lastName);
		assertThat(result.getSchoolName()).isEqualTo(schoolName);
		assertThat(result.getResidenceExtent()).isEqualTo(residenceExtent);
		assertThat(result.getDaysInHome()).isEqualTo(daysInHome);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FaChild.create()).hasAllNullFieldsOrProperties();
	}
}
