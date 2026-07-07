package se.sundsvall.caremanagement.attachments.integration.db.model;

import org.junit.jupiter.api.Test;
import org.mariadb.jdbc.MariaDbBlob;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class AttachmentDataEntityTest {

	@Test
	void testBean() {
		assertThat(AttachmentDataEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters()));
	}

	@Test
	void equalsIsIdBasedAndExcludesBlob() {
		final var entity = AttachmentDataEntity.create().withId(1).withFile(new MariaDbBlob("a".getBytes()));
		final var sameIdDifferentBlob = AttachmentDataEntity.create().withId(1).withFile(new MariaDbBlob("z".getBytes()));
		final var differentId = AttachmentDataEntity.create().withId(2);

		assertThat(entity.equals(entity)).isTrue();               // same instance
		assertThat(entity.equals(null)).isFalse();                // null
		assertThat(entity.equals("not an entity")).isFalse();     // different class
		assertThat(entity).isEqualTo(sameIdDifferentBlob)        // same id, blob excluded
			.isNotEqualTo(differentId);             // different id
		assertThat(AttachmentDataEntity.create()).isNotEqualTo(AttachmentDataEntity.create()); // two transient rows (id 0)
	}

	@Test
	void hashCodeIsConstantClassBased() {
		final var withBlob = AttachmentDataEntity.create().withId(1).withFile(new MariaDbBlob("a".getBytes()));
		final var transientEntity = AttachmentDataEntity.create();

		// constant across instances and lifecycle (transient/persisted), never reads the blob
		assertThat(withBlob).hasSameHashCodeAs(AttachmentDataEntity.class);
		assertThat(transientEntity).hasSameHashCodeAs(AttachmentDataEntity.class);
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
