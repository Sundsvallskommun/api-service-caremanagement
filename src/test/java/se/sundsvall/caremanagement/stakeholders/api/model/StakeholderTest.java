package se.sundsvall.caremanagement.stakeholders.api.model;

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
		final var contactChannels = List.of(ContactChannel.create().withKey("Email").withValue("a@b.se"));
		final var stakeholder = Stakeholder.create()
			.withId("id")
			.withExternalId("ext-1")
			.withExternalIdType("PRIVATE")
			.withRole("APPLICANT")
			.withFirstName("Joe")
			.withLastName("Doe")
			.withOrganizationName("Org")
			.withAddress("Street 1")
			.withCareOf("c/o")
			.withZipCode("00000")
			.withCity("City")
			.withCountry("Country")
			.withContactChannels(contactChannels);

		assertThat(stakeholder).hasNoNullFieldsOrProperties();
		assertThat(stakeholder.getId()).isEqualTo("id");
		assertThat(stakeholder.getExternalId()).isEqualTo("ext-1");
		assertThat(stakeholder.getExternalIdType()).isEqualTo("PRIVATE");
		assertThat(stakeholder.getRole()).isEqualTo("APPLICANT");
		assertThat(stakeholder.getFirstName()).isEqualTo("Joe");
		assertThat(stakeholder.getLastName()).isEqualTo("Doe");
		assertThat(stakeholder.getOrganizationName()).isEqualTo("Org");
		assertThat(stakeholder.getAddress()).isEqualTo("Street 1");
		assertThat(stakeholder.getCareOf()).isEqualTo("c/o");
		assertThat(stakeholder.getZipCode()).isEqualTo("00000");
		assertThat(stakeholder.getCity()).isEqualTo("City");
		assertThat(stakeholder.getCountry()).isEqualTo("Country");
		assertThat(stakeholder.getContactChannels()).isEqualTo(contactChannels);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Stakeholder.create()).hasAllNullFieldsOrProperties();
	}
}
