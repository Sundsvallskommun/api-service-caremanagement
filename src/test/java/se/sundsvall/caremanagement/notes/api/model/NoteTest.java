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
	void testNoDirtOnCreatedBean() {
		assertThat(Note.create()).hasAllNullFieldsOrProperties();
		assertThat(new Note()).hasAllNullFieldsOrProperties();
	}

}
