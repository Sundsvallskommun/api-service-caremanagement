package se.sundsvall.caremanagement.notes.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class NoteEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(NoteEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void testToString() {
		final var entity = NoteEntity.create().withId("n1").withErrandId("e1");
		assertThat(entity.toString())
			.contains("NoteEntity{").contains("id='n1'").contains("errandId='e1'");
	}

	@Test
	void testBuilderMethods() {
		final var created = FIXED_TIMESTAMP;
		final var modified = FIXED_TIMESTAMP.plusHours(1);
		final var entity = NoteEntity.create()
			.withId("n1")
			.withErrandId("e1")
			.withBody("body")
			.withAuthor("author")
			.withCreated(created)
			.withModifiedBy("editor")
			.withModified(modified);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("n1");
		assertThat(entity.getErrandId()).isEqualTo("e1");
		assertThat(entity.getBody()).isEqualTo("body");
		assertThat(entity.getAuthor()).isEqualTo("author");
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModifiedBy()).isEqualTo("editor");
		assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(NoteEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new NoteEntity()).hasAllNullFieldsOrProperties();
	}
}
