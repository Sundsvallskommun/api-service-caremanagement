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
import static org.hamcrest.core.AllOf.allOf;

class StakeholderEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		org.hamcrest.MatcherAssert.assertThat(StakeholderEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void testBuilderMethods() {
		final var contactChannels = List.of(TagEmbeddable.create().withKey("Email").withValue("a@b.se"));
		final var created = FIXED_TIMESTAMP;
		final var modified = FIXED_TIMESTAMP;

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

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandId()).isEqualTo("e1");
		assertThat(entity.getExternalId()).isEqualTo("ext");
		assertThat(entity.getExternalIdType()).isEqualTo("PRIVATE");
		assertThat(entity.getRole()).isEqualTo("APPLICANT");
		assertThat(entity.getFirstName()).isEqualTo("Joe");
		assertThat(entity.getLastName()).isEqualTo("Doe");
		assertThat(entity.getOrganizationName()).isEqualTo("Org");
		assertThat(entity.getAddress()).isEqualTo("Street 1");
		assertThat(entity.getCareOf()).isEqualTo("c/o");
		assertThat(entity.getZipCode()).isEqualTo("00000");
		assertThat(entity.getCity()).isEqualTo("City");
		assertThat(entity.getCountry()).isEqualTo("Country");
		assertThat(entity.getContactChannels()).isEqualTo(contactChannels);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(StakeholderEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new StakeholderEntity()).hasAllNullFieldsOrProperties();
	}
}
