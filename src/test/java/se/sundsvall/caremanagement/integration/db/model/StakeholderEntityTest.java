package se.sundsvall.caremanagement.integration.db.model;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class StakeholderEntityTest {

	@Test
	void testBean() {
		assertThat(StakeholderEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("errandEntity"),
			hasValidBeanEqualsExcluding("errandEntity"),
			hasValidBeanToStringExcluding("errandEntity")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = UUID.randomUUID().toString();
		final var errand = ErrandEntity.create().withId("errand-id");
		final var externalId = "190001011234";
		final var externalIdType = "PRIVATE";
		final var role = "PATIENT";
		final var firstName = "Anna";
		final var lastName = "Andersson";
		final var organizationName = "Hemtjänst";
		final var address = "Storgatan 1";
		final var careOf = "c/o";
		final var zipCode = "85100";
		final var city = "Sundsvall";
		final var country = "SE";
		final var contactChannels = List.of(TagEmbeddable.create().withKey("EMAIL").withValue("a@b.se"));
		final var parameters = List.of(StakeholderParameterEntity.create().withKey("k").withDisplayName("d").withValues(List.of("v")));

		final var entity = StakeholderEntity.create()
			.withId(id)
			.withErrandEntity(errand)
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

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getErrandEntity()).isEqualTo(errand);
		assertThat(entity.getExternalId()).isEqualTo(externalId);
		assertThat(entity.getExternalIdType()).isEqualTo(externalIdType);
		assertThat(entity.getRole()).isEqualTo(role);
		assertThat(entity.getFirstName()).isEqualTo(firstName);
		assertThat(entity.getLastName()).isEqualTo(lastName);
		assertThat(entity.getOrganizationName()).isEqualTo(organizationName);
		assertThat(entity.getAddress()).isEqualTo(address);
		assertThat(entity.getCareOf()).isEqualTo(careOf);
		assertThat(entity.getZipCode()).isEqualTo(zipCode);
		assertThat(entity.getCity()).isEqualTo(city);
		assertThat(entity.getCountry()).isEqualTo(country);
		assertThat(entity.getContactChannels()).isEqualTo(contactChannels);
		assertThat(entity.getParameters()).isEqualTo(parameters);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(StakeholderEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new StakeholderEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void toStringHandlesNullErrand() {
		assertThat(StakeholderEntity.create().toString()).contains("errandEntity=null");
	}
}
