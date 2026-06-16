package se.sundsvall.caremanagement.errandtypes.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.stakeholders.api.model.RoleDefinition;

import static org.assertj.core.api.Assertions.assertThat;

class ErrandTypeSchemaTest {

	private static final List<String> STATUSES = List.of("INKOMMEN", "UNDER_BEREDNING");
	private static final List<RoleDefinition> ROLES = List.of(new RoleDefinition("APPLICANT", "Sökande", 1, true));
	private static final List<FieldDescriptor> FIELDS = List.of(
		FieldDescriptor.create().withName("maritalStatus").withType("ENUM"));

	@Test
	void builderMethods() {
		final var schema = ErrandTypeSchema.create()
			.withTypeSlug("financial-assistance-renewal")
			.withApplicationType("RENEWAL")
			.withDisplayName("Ekonomiskt bistånd – återansökan")
			.withStatuses(STATUSES)
			.withRoles(ROLES)
			.withFields(FIELDS);

		assertThat(schema.getTypeSlug()).isEqualTo("financial-assistance-renewal");
		assertThat(schema.getApplicationType()).isEqualTo("RENEWAL");
		assertThat(schema.getDisplayName()).isEqualTo("Ekonomiskt bistånd – återansökan");
		assertThat(schema.getStatuses()).isEqualTo(STATUSES);
		assertThat(schema.getRoles()).isEqualTo(ROLES);
		assertThat(schema.getFields()).isEqualTo(FIELDS);
		assertThat(schema).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersWork() {
		final var schema = ErrandTypeSchema.create();
		schema.setTypeSlug("financial-assistance-new");
		schema.setApplicationType(null);
		schema.setDisplayName("New");
		schema.setStatuses(STATUSES);
		schema.setRoles(ROLES);
		schema.setFields(FIELDS);

		assertThat(schema.getTypeSlug()).isEqualTo("financial-assistance-new");
		assertThat(schema.getApplicationType()).isNull();
		assertThat(schema.getDisplayName()).isEqualTo("New");
		assertThat(schema.getStatuses()).isEqualTo(STATUSES);
		assertThat(schema.getRoles()).isEqualTo(ROLES);
		assertThat(schema.getFields()).isEqualTo(FIELDS);
	}

	@Test
	void equalsHashCodeAndToString() {
		final var a = ErrandTypeSchema.create().withTypeSlug("slug").withApplicationType("NEW").withStatuses(STATUSES);
		final var b = ErrandTypeSchema.create().withTypeSlug("slug").withApplicationType("NEW").withStatuses(STATUSES);
		final var c = ErrandTypeSchema.create().withTypeSlug("other");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
		assertThat(a).isEqualTo(a);
		assertThat(a).hasToString(b.toString());
		assertThat(a.toString()).contains("slug", "NEW");
	}
}
