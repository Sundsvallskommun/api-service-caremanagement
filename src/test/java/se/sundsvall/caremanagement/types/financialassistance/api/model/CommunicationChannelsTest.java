package se.sundsvall.caremanagement.types.financialassistance.api.model;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class CommunicationChannelsTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(CommunicationChannels.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var channels = CommunicationChannels.create()
			.withMinaSidor(true)
			.withDigitalMailbox(false)
			.withLetter(true);

		assertThat(channels.getMinaSidor()).isTrue();
		assertThat(channels.getDigitalMailbox()).isFalse();
		assertThat(channels.getLetter()).isTrue();
		assertThat(channels).hasNoNullFieldsOrProperties();
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(CommunicationChannels.create()).hasAllNullFieldsOrProperties();
	}
}
