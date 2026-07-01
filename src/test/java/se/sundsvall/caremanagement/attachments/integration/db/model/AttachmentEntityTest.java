package se.sundsvall.caremanagement.attachments.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class AttachmentEntityTest {
	private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.parse("2024-01-01T12:00:00Z");

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(AttachmentEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("attachmentData"),
			hasValidBeanEqualsExcluding("attachmentData")));
	}

	@Test
	void testToString() {
		final var entity = AttachmentEntity.create().withId("id").withErrandId("e1").withFileName("f.txt");
		assertThat(entity.toString())
			.contains("AttachmentEntity{").contains("id='id'").contains("errandId='e1'").contains("fileName='f.txt'");
	}

	@Test
	void testBuilderMethods() {
		final var attachmentData = AttachmentDataEntity.create();
		final var created = FIXED_TIMESTAMP;
		final var modified = FIXED_TIMESTAMP;

		final var entity = AttachmentEntity.create()
			.withId("id")
			.withErrandId("errand")
			.withNamespace("ns")
			.withMunicipalityId("mid")
			.withFileName("file.txt")
			.withMimeType("text/plain")
			.withFileSize(10)
			.withDocumentType("CONVERSATION")
			.withSenderRole("CLIENT")
			.withAttachmentData(attachmentData)
			.withCreated(created)
			.withModified(modified);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("id");
		assertThat(entity.getErrandId()).isEqualTo("errand");
		assertThat(entity.getNamespace()).isEqualTo("ns");
		assertThat(entity.getMunicipalityId()).isEqualTo("mid");
		assertThat(entity.getFileName()).isEqualTo("file.txt");
		assertThat(entity.getMimeType()).isEqualTo("text/plain");
		assertThat(entity.getFileSize()).isEqualTo(10);
		assertThat(entity.getDocumentType()).isEqualTo("CONVERSATION");
		assertThat(entity.getSenderRole()).isEqualTo("CLIENT");
		assertThat(entity.getAttachmentData()).isSameAs(attachmentData);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(AttachmentEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new AttachmentEntity()).hasAllNullFieldsOrProperties();
	}
}
