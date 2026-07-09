package se.sundsvall.caremanagement.notes.api.model;

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

class NoteTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(Note.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "n1";
		final var errandId = "e1";
		final var body = "body";
		final var author = "author";
		final var created = FIXED_TIMESTAMP;
		final var modifiedBy = "editor";
		final var modified = FIXED_TIMESTAMP.plusHours(1);

		final var note = Note.create()
			.withId(id)
			.withErrandId(errandId)
			.withBody(body)
			.withAuthor(author)
			.withCreated(created)
			.withModifiedBy(modifiedBy)
			.withModified(modified);

		assertThat(note.getId()).isEqualTo(id);
		assertThat(note.getErrandId()).isEqualTo(errandId);
		assertThat(note.getBody()).isEqualTo(body);
		assertThat(note.getAuthor()).isEqualTo(author);
		assertThat(note.getCreated()).isEqualTo(created);
		assertThat(note.getModifiedBy()).isEqualTo(modifiedBy);
		assertThat(note.getModified()).isEqualTo(modified);
	}

	@Test
	void testSetters() {
		final var note = Note.create();
		note.setId("id");
		note.setErrandId("eid");
		note.setBody("b");
		note.setAuthor("a");
		final var ts = FIXED_TIMESTAMP;
		note.setCreated(ts);
		note.setModifiedBy("editor");
		note.setModified(ts.plusHours(1));

		assertThat(note.getId()).isEqualTo("id");
		assertThat(note.getErrandId()).isEqualTo("eid");
		assertThat(note.getBody()).isEqualTo("b");
		assertThat(note.getAuthor()).isEqualTo("a");
		assertThat(note.getCreated()).isEqualTo(ts);
		assertThat(note.getModifiedBy()).isEqualTo("editor");
		assertThat(note.getModified()).isEqualTo(ts.plusHours(1));
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Note.create()).hasAllNullFieldsOrProperties();
		assertThat(new Note()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testEqualsAndHashCode() {
		final var ts = FIXED_TIMESTAMP;
		final var a = Note.create().withId("1").withErrandId("e").withBody("b").withAuthor("u").withCreated(ts).withModifiedBy("ed").withModified(ts);
		final var b = Note.create().withId("1").withErrandId("e").withBody("b").withAuthor("u").withCreated(ts).withModifiedBy("ed").withModified(ts);
		final var c = Note.create().withId("2");
		final var d = Note.create().withId("1").withErrandId("e").withBody("b").withAuthor("u").withCreated(ts).withModifiedBy("other").withModified(ts);

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(d)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}

	@Test
	void testToStringContainsFields() {
		final var note = Note.create().withId("n1").withErrandId("e1").withBody("body").withAuthor("author")
			.withCreated(FIXED_TIMESTAMP).withModifiedBy("editor").withModified(FIXED_TIMESTAMP);

		assertThat(note.toString()).contains("n1", "e1", "body", "author", "editor");
	}
}
