package se.sundsvall.caremanagement.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ErrandTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(Errand.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var municipalityId = "2281";
		final var namespace = "ns";
		final var title = "t";
		final var category = "c";
		final var type = "ty";
		final var status = "NEW";
		final var description = "d";
		final var priority = "HIGH";
		final var reporterUserId = "r";
		final var assignedUserId = "a";
		final var contactReason = "PHONE";
		final var contactReasonDescription = "crd";
		final var externalTags = List.of(ExternalTag.create());
		final var stakeholders = List.of(Stakeholder.create());
		final var parameters = List.of(Parameter.create());
		final var created = now();
		final var modified = now();
		final var touched = now();

		final var result = Errand.create()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withTitle(title)
			.withCategory(category)
			.withType(type)
			.withStatus(status)
			.withDescription(description)
			.withPriority(priority)
			.withReporterUserId(reporterUserId)
			.withAssignedUserId(assignedUserId)
			.withContactReason(contactReason)
			.withContactReasonDescription(contactReasonDescription)
			.withExternalTags(externalTags)
			.withStakeholders(stakeholders)
			.withParameters(parameters)
			.withCreated(created)
			.withModified(modified)
			.withTouched(touched);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(result.getNamespace()).isEqualTo(namespace);
		assertThat(result.getTitle()).isEqualTo(title);
		assertThat(result.getCategory()).isEqualTo(category);
		assertThat(result.getType()).isEqualTo(type);
		assertThat(result.getStatus()).isEqualTo(status);
		assertThat(result.getDescription()).isEqualTo(description);
		assertThat(result.getPriority()).isEqualTo(priority);
		assertThat(result.getReporterUserId()).isEqualTo(reporterUserId);
		assertThat(result.getAssignedUserId()).isEqualTo(assignedUserId);
		assertThat(result.getContactReason()).isEqualTo(contactReason);
		assertThat(result.getContactReasonDescription()).isEqualTo(contactReasonDescription);
		assertThat(result.getExternalTags()).isEqualTo(externalTags);
		assertThat(result.getStakeholders()).isEqualTo(stakeholders);
		assertThat(result.getParameters()).isEqualTo(parameters);
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
		assertThat(result.getTouched()).isEqualTo(touched);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Errand.create()).hasAllNullFieldsOrProperties();
	}
}
