package se.sundsvall.caremanagement.integration.db.model;

import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.MariaDbBlob;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class AttachmentDataEntityTest {

	@Test
	void testBean() {
		assertThat(AttachmentDataEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var id = 1;
		final var file = new MariaDbBlob("file".getBytes());

		final var entity = AttachmentDataEntity.create()
			.withId(id)
			.withFile(file);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getFile()).isEqualTo(file);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(AttachmentDataEntity.create()).hasAllNullFieldsOrPropertiesExcept("id");
		assertThat(new AttachmentDataEntity()).hasAllNullFieldsOrPropertiesExcept("id");
	}
}
