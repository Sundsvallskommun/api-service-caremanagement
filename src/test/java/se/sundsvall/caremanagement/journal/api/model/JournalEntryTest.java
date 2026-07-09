package se.sundsvall.caremanagement.journal.api.model;

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

class JournalEntryTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");
	private static final OffsetDateTime ENTRY_DATE_TIME = OffsetDateTime.parse("2025-05-30T14:30:00+02:00");

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(JournalEntry.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var entry = JournalEntry.create()
			.withId("je1")
			.withErrandId("e1")
			.withType("Journalfört meddelande")
			.withHeading("Rubrik")
			.withText("body")
			.withEntryDateTime(ENTRY_DATE_TIME)
			.withStatus("WORKING")
			.withCreatedBy("carola")
			.withCreated(FIXED_TIMESTAMP)
			.withModifiedBy("editor")
			.withModified(FIXED_TIMESTAMP.plusHours(1))
			.withLockedBy("locker")
			.withLocked(FIXED_TIMESTAMP.plusHours(2));

		assertThat(entry.getId()).isEqualTo("je1");
		assertThat(entry.getErrandId()).isEqualTo("e1");
		assertThat(entry.getType()).isEqualTo("Journalfört meddelande");
		assertThat(entry.getHeading()).isEqualTo("Rubrik");
		assertThat(entry.getText()).isEqualTo("body");
		assertThat(entry.getEntryDateTime()).isEqualTo(ENTRY_DATE_TIME);
		assertThat(entry.getStatus()).isEqualTo("WORKING");
		assertThat(entry.getCreatedBy()).isEqualTo("carola");
		assertThat(entry.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(entry.getModifiedBy()).isEqualTo("editor");
		assertThat(entry.getModified()).isEqualTo(FIXED_TIMESTAMP.plusHours(1));
		assertThat(entry.getLockedBy()).isEqualTo("locker");
		assertThat(entry.getLocked()).isEqualTo(FIXED_TIMESTAMP.plusHours(2));
	}

	@Test
	void testSetters() {
		final var entry = JournalEntry.create();
		entry.setId("id");
		entry.setErrandId("eid");
		entry.setType("T");
		entry.setHeading("H");
		entry.setText("b");
		entry.setEntryDateTime(ENTRY_DATE_TIME);
		entry.setStatus("LOCKED");
		entry.setCreatedBy("a");
		entry.setCreated(FIXED_TIMESTAMP);
		entry.setModifiedBy("editor");
		entry.setModified(FIXED_TIMESTAMP.plusHours(1));
		entry.setLockedBy("locker");
		entry.setLocked(FIXED_TIMESTAMP.plusHours(2));

		assertThat(entry.getId()).isEqualTo("id");
		assertThat(entry.getErrandId()).isEqualTo("eid");
		assertThat(entry.getType()).isEqualTo("T");
		assertThat(entry.getHeading()).isEqualTo("H");
		assertThat(entry.getText()).isEqualTo("b");
		assertThat(entry.getEntryDateTime()).isEqualTo(ENTRY_DATE_TIME);
		assertThat(entry.getStatus()).isEqualTo("LOCKED");
		assertThat(entry.getCreatedBy()).isEqualTo("a");
		assertThat(entry.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(entry.getModifiedBy()).isEqualTo("editor");
		assertThat(entry.getModified()).isEqualTo(FIXED_TIMESTAMP.plusHours(1));
		assertThat(entry.getLockedBy()).isEqualTo("locker");
		assertThat(entry.getLocked()).isEqualTo(FIXED_TIMESTAMP.plusHours(2));
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(JournalEntry.create()).hasAllNullFieldsOrProperties();
		assertThat(new JournalEntry()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testEqualsAndHashCode() {
		final var a = JournalEntry.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withEntryDateTime(ENTRY_DATE_TIME).withStatus("WORKING").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);
		final var b = JournalEntry.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withEntryDateTime(ENTRY_DATE_TIME).withStatus("WORKING").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);
		final var c = JournalEntry.create().withId("2");
		final var d = JournalEntry.create().withId("1").withErrandId("e").withType("T").withHeading("H").withText("b")
			.withEntryDateTime(ENTRY_DATE_TIME).withStatus("LOCKED").withCreatedBy("u").withCreated(FIXED_TIMESTAMP);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(d)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}

	@Test
	void testToStringContainsFields() {
		final var entry = JournalEntry.create()
			.withId("je1")
			.withErrandId("e1")
			.withType("Journalfört meddelande")
			.withHeading("Rubrik")
			.withText("body")
			.withStatus("WORKING")
			.withCreatedBy("carola");

		assertThat(entry.toString()).contains("je1", "e1", "Journalfört meddelande", "Rubrik", "body", "WORKING", "carola");
	}
}
