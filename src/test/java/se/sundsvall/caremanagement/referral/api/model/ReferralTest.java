package se.sundsvall.caremanagement.referral.api.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Random;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

class ReferralTest {
	private static final LocalDate SENT = LocalDate.parse("2026-06-03");
	private static final LocalDate DUE = LocalDate.parse("2026-07-01");
	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-06-03T10:00:00Z");

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt(1000)), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(Referral.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var referral = Referral.create()
			.withId("r1").withAuthority("ENVIRONMENTAL_OFFICE").withRecipient("Env").withSentAt(SENT).withDueAt(DUE)
			.withResponseText("ok").withStatus("RESPONDED").withCreated(CREATED).withModified(CREATED);

		assertThat(referral.getId()).isEqualTo("r1");
		assertThat(referral.getAuthority()).isEqualTo("ENVIRONMENTAL_OFFICE");
		assertThat(referral.getRecipient()).isEqualTo("Env");
		assertThat(referral.getSentAt()).isEqualTo(SENT);
		assertThat(referral.getDueAt()).isEqualTo(DUE);
		assertThat(referral.getResponseText()).isEqualTo("ok");
		assertThat(referral.getStatus()).isEqualTo("RESPONDED");
		assertThat(referral.getCreated()).isEqualTo(CREATED);
		assertThat(referral.getModified()).isEqualTo(CREATED);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(Referral.create()).hasAllNullFieldsOrProperties();
		assertThat(new Referral()).hasAllNullFieldsOrProperties();
	}

}
