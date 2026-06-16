package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RenewalPrefillTest {

	private static final List<PrefilledChild> CHILDREN = List.of(
		PrefilledChild.create().withPersonalNumber("201801012380").withName("Kid Andersson"));

	@Test
	void builderMethods() {
		final var prefill = RenewalPrefill.create()
			.withChildren(CHILDREN)
			.withLifecareChecked(true);

		assertThat(prefill.getChildren()).isEqualTo(CHILDREN);
		assertThat(prefill.isLifecareChecked()).isTrue();
		assertThat(prefill).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersWork() {
		final var prefill = RenewalPrefill.create();
		prefill.setChildren(CHILDREN);
		prefill.setLifecareChecked(false);

		assertThat(prefill.getChildren()).isEqualTo(CHILDREN);
		assertThat(prefill.isLifecareChecked()).isFalse();
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(RenewalPrefill.create()).hasAllNullFieldsOrPropertiesExcept("lifecareChecked");
	}

	@Test
	void equalsAndHashCode() {
		final var a = RenewalPrefill.create().withChildren(CHILDREN).withLifecareChecked(true);
		final var b = RenewalPrefill.create().withChildren(CHILDREN).withLifecareChecked(true);
		final var c = RenewalPrefill.create().withLifecareChecked(false);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
	}
}
