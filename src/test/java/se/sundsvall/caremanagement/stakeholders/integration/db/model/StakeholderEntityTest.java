package se.sundsvall.caremanagement.stakeholders.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class StakeholderEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(StakeholderEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void testBuilderMethods() {
		final var contactChannels = List.of(TagEmbeddable.create().withKey("Email").withValue("a@b.se"));
		final var created = now();
		final var modified = now();

		final var entity = StakeholderEntity.create()
			.withId("id")
			.withErrandId("e1")
			.withExternalId("ext")
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
			.withContactChannels(contactChannels)
			.withCreated(created)
			.withModified(modified);

		org.assertj.core.api.Assertions.assertThat(entity).hasNoNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(entity.getId()).isEqualTo("id");
		org.assertj.core.api.Assertions.assertThat(entity.getErrandId()).isEqualTo("e1");
		org.assertj.core.api.Assertions.assertThat(entity.getExternalId()).isEqualTo("ext");
		org.assertj.core.api.Assertions.assertThat(entity.getExternalIdType()).isEqualTo("PRIVATE");
		org.assertj.core.api.Assertions.assertThat(entity.getRole()).isEqualTo("APPLICANT");
		org.assertj.core.api.Assertions.assertThat(entity.getFirstName()).isEqualTo("Joe");
		org.assertj.core.api.Assertions.assertThat(entity.getLastName()).isEqualTo("Doe");
		org.assertj.core.api.Assertions.assertThat(entity.getOrganizationName()).isEqualTo("Org");
		org.assertj.core.api.Assertions.assertThat(entity.getAddress()).isEqualTo("Street 1");
		org.assertj.core.api.Assertions.assertThat(entity.getCareOf()).isEqualTo("c/o");
		org.assertj.core.api.Assertions.assertThat(entity.getZipCode()).isEqualTo("00000");
		org.assertj.core.api.Assertions.assertThat(entity.getCity()).isEqualTo("City");
		org.assertj.core.api.Assertions.assertThat(entity.getCountry()).isEqualTo("Country");
		org.assertj.core.api.Assertions.assertThat(entity.getContactChannels()).isEqualTo(contactChannels);
		org.assertj.core.api.Assertions.assertThat(entity.getCreated()).isEqualTo(created);
		org.assertj.core.api.Assertions.assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		org.assertj.core.api.Assertions.assertThat(StakeholderEntity.create()).hasAllNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(new StakeholderEntity()).hasAllNullFieldsOrProperties();
	}
}
