package se.sundsvall.caremanagement.types.financialassistance.api.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RenewalPrefillTest {

	private static final List<PrefillPerson> PERSONS = List.of(
		PrefillPerson.create().withRole("APPLICANT").withPersonalNumber("198001012389").withName("Anna Andersson"));
	private static final List<PrefillPerson> CHILDREN = List.of(
		PrefillPerson.create().withPersonalNumber("201801012380").withName("Kid Andersson"));

	@Test
	void builderMethods() {
		final var prefill = RenewalPrefill.create()
			.withPersons(PERSONS)
			.withChildren(CHILDREN)
			.withLifecareChecked(true);

		assertThat(prefill.getPersons()).isEqualTo(PERSONS);
		assertThat(prefill.getChildren()).isEqualTo(CHILDREN);
		assertThat(prefill.isLifecareChecked()).isTrue();
		assertThat(prefill).hasNoNullFieldsOrProperties();
	}

	@Test
	void settersWork() {
		final var prefill = RenewalPrefill.create();
		prefill.setPersons(PERSONS);
		prefill.setChildren(CHILDREN);
		prefill.setLifecareChecked(false);

		assertThat(prefill.getPersons()).isEqualTo(PERSONS);
		assertThat(prefill.getChildren()).isEqualTo(CHILDREN);
		assertThat(prefill.isLifecareChecked()).isFalse();
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(RenewalPrefill.create()).hasAllNullFieldsOrPropertiesExcept("lifecareChecked");
	}

	@Test
	void equalsAndHashCode() {
		final var a = RenewalPrefill.create().withPersons(PERSONS).withLifecareChecked(true);
		final var b = RenewalPrefill.create().withPersons(PERSONS).withLifecareChecked(true);
		final var c = RenewalPrefill.create().withLifecareChecked(false);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
	}
}
