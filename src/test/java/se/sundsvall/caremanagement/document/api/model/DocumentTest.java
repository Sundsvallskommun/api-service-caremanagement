package se.sundsvall.caremanagement.document.api.model;

import java.time.LocalDate;
import java.time.LocalTime;
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
	private static final LocalDate DOCUMENT_DATE = LocalDate.parse("2025-05-30");
	private static final LocalTime DOCUMENT_TIME = LocalTime.of(14, 30);

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1000)), LocalDate.class);
		registerValueGenerator(() -> LocalTime.ofNanoOfDay(Math.floorMod(new Random().nextLong(), 86_400_000_000_000L)), LocalTime.class);
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
			.withDocumentDate(DOCUMENT_DATE)
			.withDocumentTime(DOCUMENT_TIME)
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
		assertThat(document.getDocumentDate()).isEqualTo(DOCUMENT_DATE);
		assertThat(document.getDocumentTime()).isEqualTo(DOCUMENT_TIME);
		assertThat(document.getStatus()).isEqualTo("WORKING");
		assertThat(document.getCreatedBy()).isEqualTo("carola");
		assertThat(document.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(document.getModifiedBy()).isEqualTo("editor");
		assertThat(document.getModified()).isEqualTo(FIXED_TIMESTAMP.plusHours(1));
		assertThat(document.getLockedBy()).isEqualTo("locker");
		assertThat(document.getLocked()).isEqualTo(FIXED_TIMESTAMP.plusHours(2));
	}

	@Test
	void testSetters() {
		final var document = Document.create();
		document.setId("id");
		document.setErrandId("eid");
		document.setType("T");
		document.setHeading("H");
		document.setText("b");
		document.setDocumentDate(DOCUMENT_DATE);
		document.setDocumentTime(DOCUMENT_TIME);
		document.setStatus("LOCKED");
		document.setCreatedBy("a");
		document.setCreated(FIXED_TIMESTAMP);
		document.setModifiedBy("editor");
		document.setModified(FIXED_TIMESTAMP.plusHours(1));
		document.setLockedBy("locker");
		document.setLocked(FIXED_TIMESTAMP.plusHours(2));

		assertThat(document.getId()).isEqualTo("id");
		assertThat(document.getErrandId()).isEqualTo("eid");
		assertThat(document.getType()).isEqualTo("T");
		assertThat(document.getHeading()).isEqualTo("H");
		assertThat(document.getText()).isEqualTo("b");
		assertThat(document.getDocumentDate()).isEqualTo(DOCUMENT_DATE);
		assertThat(document.getDocumentTime()).isEqualTo(DOCUMENT_TIME);
		assertThat(document.getStatus()).isEqualTo("LOCKED");
		assertThat(document.getCreatedBy()).isEqualTo("a");
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

	@Test
	void testEqualsAndHashCode() {
		final var a = Document.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withDocumentDate(DOCUMENT_DATE).withDocumentTime(DOCUMENT_TIME).withStatus("WORKING").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);
		final var b = Document.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withDocumentDate(DOCUMENT_DATE).withDocumentTime(DOCUMENT_TIME).withStatus("WORKING").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);
		final var c = Document.create().withId("2");
		final var d = Document.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withDocumentDate(DOCUMENT_DATE).withDocumentTime(DOCUMENT_TIME).withStatus("LOCKED").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(d)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}

	@Test
	void testToStringContainsFields() {
		final var document = Document.create()
			.withId("d1")
			.withErrandId("e1")
			.withType("Brev")
			.withHeading("Rubrik")
			.withText("body")
			.withStatus("WORKING")
			.withCreatedBy("carola");

		assertThat(document.toString()).contains("d1", "e1", "Brev", "Rubrik", "body", "WORKING", "carola");
	}
}
