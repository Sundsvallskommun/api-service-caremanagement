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

class CreateLifecareJournalNoteRequestTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(CreateLifecareJournalNoteRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var result = CreateLifecareJournalNoteRequest.create()
			.withContent("<p>Hej</p>")
			.withNoteTypeCode(1)
			.withTitle("Telefonsamtal")
			.withOccurenceTime("11:50")
			.withOccurenceDate("2026-09-23")
			.withWriteProtected(true);

		assertThat(result).hasNoNullFieldsOrProperties();
		assertThat(result.getContent()).isEqualTo("<p>Hej</p>");
		assertThat(result.getNoteTypeCode()).isEqualTo(1);
		assertThat(result.getTitle()).isEqualTo("Telefonsamtal");
		assertThat(result.getOccurenceTime()).isEqualTo("11:50");
		assertThat(result.getOccurenceDate()).isEqualTo("2026-09-23");
		assertThat(result.getWriteProtected()).isEqualTo(true);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CreateLifecareJournalNoteRequest.create()).hasAllNullFieldsOrProperties();
	}
}
