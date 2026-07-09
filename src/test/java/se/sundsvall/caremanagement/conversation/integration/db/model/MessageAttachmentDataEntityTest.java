package se.sundsvall.caremanagement.conversation.integration.db.model;

import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.MariaDbBlob;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToStringExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class MessageAttachmentDataEntityTest {

	@Test
	void testBean() {
		assertThat(MessageAttachmentDataEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("file"),
			hasValidBeanEqualsExcluding("file"),
			hasValidBeanToStringExcluding("file")));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = 1;
		final var messageAttachmentId = "attachment-1";
		final var file = new MariaDbBlob("file".getBytes());

		final var entity = MessageAttachmentDataEntity.create()
			.withId(id)
			.withMessageAttachmentId(messageAttachmentId)
			.withFile(file);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMessageAttachmentId()).isEqualTo(messageAttachmentId);
		assertThat(entity.getFile()).isEqualTo(file);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MessageAttachmentDataEntity.create()).hasAllNullFieldsOrPropertiesExcept("id");
		assertThat(new MessageAttachmentDataEntity()).hasAllNullFieldsOrPropertiesExcept("id");
	}
}
