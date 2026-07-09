package se.sundsvall.caremanagement.referral.integration.db.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Random;
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
import static org.hamcrest.MatcherAssert.assertThat;

class ReferralEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt()), LocalDate.class);
	}

	@Test
	void testBean() {
		assertThat(ReferralEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void hasValidBuilderMethods() {
		final var sent = LocalDate.parse("2026-06-03");
		final var created = OffsetDateTime.parse("2026-06-03T10:00:00Z");

		final var entity = ReferralEntity.create()
			.withId("r1").withErrandId("e1").withAuthority("ENVIRONMENTAL_OFFICE").withRecipient("Env").withSentAt(sent)
			.withDueAt(sent.plusWeeks(4)).withResponseText("ok").withStatus("SENT").withCreated(created).withModified(created);

		assertThat(entity).hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo("r1");
		assertThat(entity.getErrandId()).isEqualTo("e1");
		assertThat(entity.getAuthority()).isEqualTo("ENVIRONMENTAL_OFFICE");
		assertThat(entity.getRecipient()).isEqualTo("Env");
		assertThat(entity.getSentAt()).isEqualTo(sent);
		assertThat(entity.getDueAt()).isEqualTo(sent.plusWeeks(4));
		assertThat(entity.getResponseText()).isEqualTo("ok");
		assertThat(entity.getStatus()).isEqualTo("SENT");
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(created);
	}

	@Test
	void hasNoDirtOnCreatedBean() {
		assertThat(ReferralEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ReferralEntity()).hasAllNullFieldsOrProperties();
	}
}
