package se.sundsvall.caremanagement.document.api.model;

import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class DocumentTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");
	private static final OffsetDateTime DOCUMENT_DATE_TIME = OffsetDateTime.parse("2025-05-30T14:30:00+02:00");

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(Document.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var document = Document.create()
			.withId("d1")
			.withErrandId("e1")
			.withType("Brev")
			.withHeading("Rubrik")
			.withText("body")
			.withDocumentDateTime(DOCUMENT_DATE_TIME)
			.withStatus("WORKING")
			.withCreatedBy("carola")
			.withCreated(FIXED_TIMESTAMP)
			.withModifiedBy("editor")
			.withModified(FIXED_TIMESTAMP.plusHours(1))
			.withLockedBy("locker")
			.withLocked(FIXED_TIMESTAMP.plusHours(2));

		assertThat(document.getId()).isEqualTo("d1");
		assertThat(document.getErrandId()).isEqualTo("e1");
		assertThat(document.getType()).isEqualTo("Brev");
		assertThat(document.getHeading()).isEqualTo("Rubrik");
		assertThat(document.getText()).isEqualTo("body");
		assertThat(document.getDocumentDateTime()).isEqualTo(DOCUMENT_DATE_TIME);
		assertThat(document.getStatus()).isEqualTo("WORKING");
		assertThat(document.getCreatedBy()).isEqualTo("carola");
		assertThat(document.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(document.getModifiedBy()).isEqualTo("editor");
		assertThat(document.getModified()).isEqualTo(FIXED_TIMESTAMP.plusHours(1));
		assertThat(document.getLockedBy()).isEqualTo("locker");
		assertThat(document.getLocked()).isEqualTo(FIXED_TIMESTAMP.plusHours(2));
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Document.create()).hasAllNullFieldsOrProperties();
		assertThat(new Document()).hasAllNullFieldsOrProperties();
	}
}
