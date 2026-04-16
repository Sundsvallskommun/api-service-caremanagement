package se.sundsvall.caremanagement.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class StakeholderTest {

	@Test
	void testBean() {
		assertThat(Stakeholder.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var externalId = "externalId";
		final var externalIdType = "PRIVATE";
		final var role = "APPLICANT";
		final var firstName = "Joe";
		final var lastName = "Doe";
		final var organizationName = "Org";
		final var address = "Storgatan 1";
		final var careOf = "c/o";
		final var zipCode = "12345";
		final var city = "Sundsvall";
		final var country = "Sweden";
		final var contactChannels = List.of(ContactChannel.create().withKey("k").withValue("v"));
		final var parameters = List.of(StakeholderParameter.create().withKey("k"));

		final var result = Stakeholder.create()
			.withId(id)
			.withExternalId(externalId)
			.withExternalIdType(externalIdType)
			.withRole(role)
			.withFirstName(firstName)
			.withLastName(lastName)
			.withOrganizationName(organizationName)
			.withAddress(address)
			.withCareOf(careOf)
			.withZipCode(zipCode)
			.withCity(city)
			.withCountry(country)
			.withContactChannels(contactChannels)
			.withParameters(parameters);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getExternalId()).isEqualTo(externalId);
		assertThat(result.getExternalIdType()).isEqualTo(externalIdType);
		assertThat(result.getRole()).isEqualTo(role);
		assertThat(result.getFirstName()).isEqualTo(firstName);
		assertThat(result.getLastName()).isEqualTo(lastName);
		assertThat(result.getOrganizationName()).isEqualTo(organizationName);
		assertThat(result.getAddress()).isEqualTo(address);
		assertThat(result.getCareOf()).isEqualTo(careOf);
		assertThat(result.getZipCode()).isEqualTo(zipCode);
		assertThat(result.getCity()).isEqualTo(city);
		assertThat(result.getCountry()).isEqualTo(country);
		assertThat(result.getContactChannels()).isEqualTo(contactChannels);
		assertThat(result.getParameters()).isEqualTo(parameters);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Stakeholder.create()).hasAllNullFieldsOrProperties();
	}
}
