package se.sundsvall.caremanagement.document.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static se.sundsvall.caremanagement.document.integration.db.model.DocumentStatus.WORKING;

class DocumentEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1000)), LocalDate.class);
		BeanMatchers.registerValueGenerator(() -> LocalTime.ofNanoOfDay(Math.floorMod(new Random().nextLong(), 86_400_000_000_000L)), LocalTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(DocumentEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("text"),
			hasValidBeanEqualsExcluding("text"),
			hasValidBeanToStringExcluding("text")));
	}

	@Test
	void testBuilderMethods() {
		final var documentDate = LocalDate.parse("2025-05-30");
		final var documentTime = LocalTime.of(14, 30);
		final var modified = FIXED_TIMESTAMP.plusHours(1);
		final var locked = FIXED_TIMESTAMP.plusHours(2);

		final var entity = DocumentEntity.create()
			.withId("d1")
			.withErrandId("e1")
			.withType("Brev")
			.withHeading("Rubrik")
			.withText("body")
			.withDocumentDate(documentDate)
			.withDocumentTime(documentTime)
			.withStatus(WORKING)
			.withCreatedBy("carola")
			.withCreated(FIXED_TIMESTAMP)
			.withModifiedBy("editor")
			.withModified(modified)
			.withLockedBy("locker")
			.withLocked(locked);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("d1");
		assertThat(entity.getErrandId()).isEqualTo("e1");
		assertThat(entity.getType()).isEqualTo("Brev");
		assertThat(entity.getHeading()).isEqualTo("Rubrik");
		assertThat(entity.getText()).isEqualTo("body");
		assertThat(entity.getDocumentDate()).isEqualTo(documentDate);
		assertThat(entity.getDocumentTime()).isEqualTo(documentTime);
		assertThat(entity.getStatus()).isEqualTo(WORKING);
		assertThat(entity.getCreatedBy()).isEqualTo("carola");
		assertThat(entity.getCreated()).isEqualTo(FIXED_TIMESTAMP);
		assertThat(entity.getModifiedBy()).isEqualTo("editor");
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getLockedBy()).isEqualTo("locker");
		assertThat(entity.getLocked()).isEqualTo(locked);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(DocumentEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new DocumentEntity()).hasAllNullFieldsOrProperties();
	}
}
