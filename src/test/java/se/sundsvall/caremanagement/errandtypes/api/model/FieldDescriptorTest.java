package se.sundsvall.caremanagement.errandtypes.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldDescriptorTest {

	@Test
	void builderMethods() {
		final var field = FieldDescriptor.create()
			.withName("maritalStatus")
			.withType("ENUM")
			.withRequired(true)
			.withOptions(List.of("SINGLE", "COHABITING"))
			.withItemsRef(null)
			.withAppliesTo(List.of("NEW", "RENEWAL"))
			.withCondition("periodChoice == OTHER_BENEFIT")
			.withDescription("Marital status of the applicant");

		assertThat(field.getName()).isEqualTo("maritalStatus");
		assertThat(field.getType()).isEqualTo("ENUM");
		assertThat(field.isRequired()).isTrue();
		assertThat(field.getOptions()).containsExactly("SINGLE", "COHABITING");
		assertThat(field.getItemsRef()).isNull();
		assertThat(field.getAppliesTo()).containsExactly("NEW", "RENEWAL");
		assertThat(field.getCondition()).isEqualTo("periodChoice == OTHER_BENEFIT");
		assertThat(field.getDescription()).isEqualTo("Marital status of the applicant");
	}

	@Test
	void settersWork() {
		final var field = FieldDescriptor.create();
		field.setName("costs");
		field.setType("ARRAY");
		field.setRequired(false);
		field.setOptions(null);
		field.setItemsRef("Cost");
		field.setAppliesTo(List.of("NEW", "RENEWAL", "SUPPLEMENTARY"));
		field.setCondition(null);
		field.setDescription("Costs applied for");

		assertThat(field.getName()).isEqualTo("costs");
		assertThat(field.getType()).isEqualTo("ARRAY");
		assertThat(field.isRequired()).isFalse();
		assertThat(field.getOptions()).isNull();
		assertThat(field.getItemsRef()).isEqualTo("Cost");
		assertThat(field.getAppliesTo()).containsExactly("NEW", "RENEWAL", "SUPPLEMENTARY");
		assertThat(field.getCondition()).isNull();
		assertThat(field.getDescription()).isEqualTo("Costs applied for");
	}

	@Test
	void equalsHashCodeAndToString() {
		final var a = FieldDescriptor.create().withName("hasIncomes").withType("BOOLEAN").withRequired(true)
			.withAppliesTo(List.of("NEW", "RENEWAL"));
		final var b = FieldDescriptor.create().withName("hasIncomes").withType("BOOLEAN").withRequired(true)
			.withAppliesTo(List.of("NEW", "RENEWAL"));
		final var c = FieldDescriptor.create().withName("hasAssets").withType("BOOLEAN").withRequired(false);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
		assertThat(a).isEqualTo(a);
		assertThat(a).hasToString(b.toString());
		assertThat(a.toString()).contains("hasIncomes", "BOOLEAN");
	}
}
