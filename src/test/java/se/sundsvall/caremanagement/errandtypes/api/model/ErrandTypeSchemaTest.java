package se.sundsvall.caremanagement.errandtypes.api.model;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.stakeholders.api.model.RoleDefinition;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class ErrandTypeSchemaTest {

	private static final List<StatusDefinition> STATUSES = List.of(
		new StatusDefinition("RECEIVED", "Inkommen"), new StatusDefinition("UNDER_REVIEW", "Under utredning"));
	private static final List<RoleDefinition> ROLES = List.of(new RoleDefinition("APPLICANT", "Sökande", 1, true));
	private static final List<FieldDescriptor> FIELDS = List.of(
		FieldDescriptor.create().withName("maritalStatus").withType("ENUM"));
	private static final List<DecisionOption> DECISION_OPTIONS = List.of(
		DecisionOption.create().withCode("BIFALL").withDisplayName("Bifall").withCarriesAmount(true));

	@Test
	void testBean() {
		MatcherAssert.assertThat(ErrandTypeSchema.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var schema = ErrandTypeSchema.create()
			.withTypeSlug("financial-assistance-renewal")
			.withApplicationType("RENEWAL")
			.withDisplayName("Ekonomiskt bistånd – återansökan")
			.withStatuses(STATUSES)
			.withRoles(ROLES)
			.withFields(FIELDS)
			.withDecisionOptions(DECISION_OPTIONS);

		assertThat(schema.getTypeSlug()).isEqualTo("financial-assistance-renewal");
		assertThat(schema.getApplicationType()).isEqualTo("RENEWAL");
		assertThat(schema.getDisplayName()).isEqualTo("Ekonomiskt bistånd – återansökan");
		assertThat(schema.getStatuses()).isEqualTo(STATUSES);
		assertThat(schema.getRoles()).isEqualTo(ROLES);
		assertThat(schema.getFields()).isEqualTo(FIELDS);
		assertThat(schema.getDecisionOptions()).isEqualTo(DECISION_OPTIONS);
		assertThat(schema).hasNoNullFieldsOrProperties();
	}

	@Test
	void testSettersWork() {
		final var schema = ErrandTypeSchema.create();
		schema.setTypeSlug("financial-assistance-new");
		schema.setApplicationType(null);
		schema.setDisplayName("New");
		schema.setStatuses(STATUSES);
		schema.setRoles(ROLES);
		schema.setFields(FIELDS);
		schema.setDecisionOptions(DECISION_OPTIONS);

		assertThat(schema.getTypeSlug()).isEqualTo("financial-assistance-new");
		assertThat(schema.getApplicationType()).isNull();
		assertThat(schema.getDisplayName()).isEqualTo("New");
		assertThat(schema.getStatuses()).isEqualTo(STATUSES);
		assertThat(schema.getRoles()).isEqualTo(ROLES);
		assertThat(schema.getFields()).isEqualTo(FIELDS);
		assertThat(schema.getDecisionOptions()).isEqualTo(DECISION_OPTIONS);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(ErrandTypeSchema.create()).hasAllNullFieldsOrProperties();
		assertThat(new ErrandTypeSchema()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testEqualsHashCodeAndToString() {
		final var a = ErrandTypeSchema.create().withTypeSlug("slug").withApplicationType("NEW").withStatuses(STATUSES);
		final var b = ErrandTypeSchema.create().withTypeSlug("slug").withApplicationType("NEW").withStatuses(STATUSES);
		final var c = ErrandTypeSchema.create().withTypeSlug("other");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(null)
			.isNotEqualTo("string")
			.isEqualTo(a)
			.hasToString(b.toString());
		assertThat(a.toString()).contains("slug", "NEW");
	}
}
