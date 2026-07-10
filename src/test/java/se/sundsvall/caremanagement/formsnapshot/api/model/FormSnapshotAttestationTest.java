package se.sundsvall.caremanagement.formsnapshot.api.model;

import com.google.code.beanmatchers.BeanMatchers;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class FormSnapshotAttestationTest {

	@BeforeAll
	static void setup() {
		final var random = new Random();
		BeanMatchers.registerValueGenerator(() -> FormSnapshotAnswer.create().withDisplay("D" + random.nextInt()), FormSnapshotAnswer.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(FormSnapshotAttestation.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var answer = FormSnapshotAnswer.create().withDisplay("Ja");
		final var attestation = FormSnapshotAttestation.create()
			.withLabel("Jag intygar")
			.withAnswer(answer);

		assertThat(attestation.getLabel()).isEqualTo("Jag intygar");
		assertThat(attestation.getAnswer()).isEqualTo(answer);
	}
}
