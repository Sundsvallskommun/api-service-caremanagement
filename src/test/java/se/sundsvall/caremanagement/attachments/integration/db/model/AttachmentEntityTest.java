package se.sundsvall.caremanagement.attachments.integration.db.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class AttachmentEntityTest {

	@BeforeAll
	static void setup() {
		BeanMatchers.registerValueGenerator(() -> now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(AttachmentEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void testToString() {
		final var entity = AttachmentEntity.create().withId("id").withErrandId("e1").withFileName("f.txt");
		org.assertj.core.api.Assertions.assertThat(entity.toString())
			.contains("AttachmentEntity{").contains("id='id'").contains("errandId='e1'").contains("fileName='f.txt'");
	}

	@Test
	void testBuilderMethods() {
		final var attachmentData = AttachmentDataEntity.create();
		final var created = now();
		final var modified = now();

		final var entity = AttachmentEntity.create()
			.withId("id")
			.withErrandId("errand")
			.withNamespace("ns")
			.withMunicipalityId("mid")
			.withFileName("file.txt")
			.withMimeType("text/plain")
			.withFileSize(10)
			.withAttachmentData(attachmentData)
			.withCreated(created)
			.withModified(modified);

		org.assertj.core.api.Assertions.assertThat(entity).hasNoNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(entity.getId()).isEqualTo("id");
		org.assertj.core.api.Assertions.assertThat(entity.getErrandId()).isEqualTo("errand");
		org.assertj.core.api.Assertions.assertThat(entity.getNamespace()).isEqualTo("ns");
		org.assertj.core.api.Assertions.assertThat(entity.getMunicipalityId()).isEqualTo("mid");
		org.assertj.core.api.Assertions.assertThat(entity.getFileName()).isEqualTo("file.txt");
		org.assertj.core.api.Assertions.assertThat(entity.getMimeType()).isEqualTo("text/plain");
		org.assertj.core.api.Assertions.assertThat(entity.getFileSize()).isEqualTo(10);
		org.assertj.core.api.Assertions.assertThat(entity.getAttachmentData()).isSameAs(attachmentData);
		org.assertj.core.api.Assertions.assertThat(entity.getCreated()).isEqualTo(created);
		org.assertj.core.api.Assertions.assertThat(entity.getModified()).isEqualTo(modified);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		org.assertj.core.api.Assertions.assertThat(AttachmentEntity.create()).hasAllNullFieldsOrProperties();
		org.assertj.core.api.Assertions.assertThat(new AttachmentEntity()).hasAllNullFieldsOrProperties();
	}
}
