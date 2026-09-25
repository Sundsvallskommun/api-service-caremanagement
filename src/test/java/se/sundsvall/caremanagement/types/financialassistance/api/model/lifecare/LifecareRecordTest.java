package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class LifecareRecordTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(LifecareRecord.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = LifecareRecord.create()
			.withId("138")
			.withCategory("JOURNAL_NOTE")
			.withTitle("Journalanteckning")
			.withDateTime("2026-09-23T12:11")
			.withType("Journalanteckning")
			.withOwnerTypeText("EK Ekonomiskt bistånd")
			.withResponsibleCaseworker("RPA_031DEV")
			.withModifiedBy("RPA_031DEV 2026-09-23")
			.withLocked(true)
			.withWriteProtected(true);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo("138");
		assertThat(result.getCategory()).isEqualTo("JOURNAL_NOTE");
		assertThat(result.getTitle()).isEqualTo("Journalanteckning");
		assertThat(result.getDateTime()).isEqualTo("2026-09-23T12:11");
		assertThat(result.getType()).isEqualTo("Journalanteckning");
		assertThat(result.getOwnerTypeText()).isEqualTo("EK Ekonomiskt bistånd");
		assertThat(result.getResponsibleCaseworker()).isEqualTo("RPA_031DEV");
		assertThat(result.getModifiedBy()).isEqualTo("RPA_031DEV 2026-09-23");
		assertThat(result.getLocked()).isEqualTo(true);
		assertThat(result.getWriteProtected()).isEqualTo(true);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(LifecareRecord.create()).hasAllNullFieldsOrProperties();
	}
}
