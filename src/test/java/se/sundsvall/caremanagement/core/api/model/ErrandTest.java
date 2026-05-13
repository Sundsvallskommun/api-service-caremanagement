package se.sundsvall.caremanagement.core.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
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
		final var created = now();
		final var modified = now();
		final var touched = now();

		final var errand = Errand.create()
			.withId("id")
			.withMunicipalityId("2281")
			.withNamespace("MY_NAMESPACE")
			.withErrandNumber("CAREM-2026-1")
			.withTypeSlug("type")
			.withTitle("title")
			.withStatus("OPEN")
			.withDescription("desc")
			.withPriority("HIGH")
			.withReporterUserId("reporter")
			.withAssignedUserId("assignee")
			.withProcessDefinitionName("BPMN")
			.withProcessInstanceId("pi-1")
			.withCreated(created)
			.withModified(modified)
			.withTouched(touched);

		assertThat(errand).hasNoNullFieldsOrProperties();
		assertThat(errand.getId()).isEqualTo("id");
		assertThat(errand.getMunicipalityId()).isEqualTo("2281");
		assertThat(errand.getNamespace()).isEqualTo("MY_NAMESPACE");
		assertThat(errand.getErrandNumber()).isEqualTo("CAREM-2026-1");
		assertThat(errand.getTypeSlug()).isEqualTo("type");
		assertThat(errand.getTitle()).isEqualTo("title");
		assertThat(errand.getStatus()).isEqualTo("OPEN");
		assertThat(errand.getDescription()).isEqualTo("desc");
		assertThat(errand.getPriority()).isEqualTo("HIGH");
		assertThat(errand.getReporterUserId()).isEqualTo("reporter");
		assertThat(errand.getAssignedUserId()).isEqualTo("assignee");
		assertThat(errand.getProcessDefinitionName()).isEqualTo("BPMN");
		assertThat(errand.getProcessInstanceId()).isEqualTo("pi-1");
		assertThat(errand.getCreated()).isEqualTo(created);
		assertThat(errand.getModified()).isEqualTo(modified);
		assertThat(errand.getTouched()).isEqualTo(touched);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Errand.create()).hasAllNullFieldsOrProperties();
	}
}
