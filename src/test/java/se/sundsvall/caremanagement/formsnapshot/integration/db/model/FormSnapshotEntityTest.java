package se.sundsvall.caremanagement.formsnapshot.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class FormSnapshotEntityTest {

	private static final OffsetDateTime CAPTURED_AT = OffsetDateTime.parse("2026-06-24T10:15:30+02:00");
	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-06-24T08:15:31Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FormSnapshotEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void testToStringOmitsPayload() {
		final var entity = FormSnapshotEntity.create().withId("fs1").withErrandId("e1").withPayload("SENSITIVE-PERSONAL-DATA");

		assertThat(entity.toString())
			.contains("FormSnapshotEntity{").contains("id='fs1'").contains("errandId='e1'")
			.doesNotContain("SENSITIVE-PERSONAL-DATA");
	}

	@Test
	void testBuilderMethods() {
		final var entity = FormSnapshotEntity.create()
			.withId("fs1")
			.withErrandId("e1")
			.withMunicipalityId("2281")
			.withNamespace("EB")
			.withTypeSlug("financial-assistance-new")
			.withSchemaVersion("form-snapshot/1")
			.withFormDefinitionVersion("eb-2026.06-r3")
			.withLocale("sv-SE")
			.withContentHash("abc123")
			.withPayload("{\"schemaVersion\":\"form-snapshot/1\",\"sections\":[]}")
			.withCapturedAt(CAPTURED_AT)
			.withCreated(CREATED);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("fs1");
		assertThat(entity.getErrandId()).isEqualTo("e1");
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("EB");
		assertThat(entity.getTypeSlug()).isEqualTo("financial-assistance-new");
		assertThat(entity.getSchemaVersion()).isEqualTo("form-snapshot/1");
		assertThat(entity.getFormDefinitionVersion()).isEqualTo("eb-2026.06-r3");
		assertThat(entity.getLocale()).isEqualTo("sv-SE");
		assertThat(entity.getContentHash()).isEqualTo("abc123");
		assertThat(entity.getPayload()).isEqualTo("{\"schemaVersion\":\"form-snapshot/1\",\"sections\":[]}");
		assertThat(entity.getCapturedAt()).isEqualTo(CAPTURED_AT);
		assertThat(entity.getCreated()).isEqualTo(CREATED);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(FormSnapshotEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new FormSnapshotEntity()).hasAllNullFieldsOrProperties();
	}
}
