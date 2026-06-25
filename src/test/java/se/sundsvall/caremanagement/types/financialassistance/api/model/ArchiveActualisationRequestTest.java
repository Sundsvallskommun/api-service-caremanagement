package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

class ArchiveActualisationRequestTest {

	@Test
	void testBean() {
		assertThat(ArchiveActualisationRequest.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void builderMethods() {
		final var request = ArchiveActualisationRequest.create()
			.withTitle("Tilläggsansökan")
			.withDocumentType("ANSOKAN")
			.withDocumentSenderType("ENSKILD")
			.withSenderName("Draken");

		assertThat(request.getTitle()).isEqualTo("Tilläggsansökan");
		assertThat(request.getDocumentType()).isEqualTo("ANSOKAN");
		assertThat(request.getDocumentSenderType()).isEqualTo("ENSKILD");
		assertThat(request.getSenderName()).isEqualTo("Draken");
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(ArchiveActualisationRequest.create()).hasAllNullFieldsOrProperties();
	}
}
