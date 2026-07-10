package se.sundsvall.caremanagement.formsnapshot.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import org.hamcrest.MatcherAssert;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static java.time.OffsetDateTime.now;
import static org.hamcrest.CoreMatchers.allOf;

final class FormSnapshotModelTestSupport {

	private FormSnapshotModelTestSupport() {}

	static void assertValidBean(final Class<?> type) {
		MatcherAssert.assertThat(type, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	static void registerValueGenerators() {
		final var random = new Random();
		BeanMatchers.registerValueGenerator(() -> now().plusDays(random.nextInt()), OffsetDateTime.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotAnswer.create().withDisplay("D" + random.nextInt()), FormSnapshotAnswer.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotOption.create().withCode("C" + random.nextInt()), FormSnapshotOption.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotNotice.create().withText("T" + random.nextInt()), FormSnapshotNotice.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotField.create().withName("n" + random.nextInt()), FormSnapshotField.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotGroup.create().withFields(List.of(FormSnapshotField.create().withName("g" + random.nextInt()))), FormSnapshotGroup.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotSection.create().withId("s" + random.nextInt()), FormSnapshotSection.class);
		BeanMatchers.registerValueGenerator(() -> FormSnapshotAttestation.create().withLabel("A" + random.nextInt()), FormSnapshotAttestation.class);
	}
}
