package se.sundsvall.caremanagement.referral.integration.db.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferralEntityTest {
	private static final LocalDate SENT = LocalDate.parse("2026-06-03");
	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-06-03T10:00:00Z");

	@Test
	void builderMethods() {
		final var entity = ReferralEntity.create()
			.withId("r1").withErrandId("e1").withAuthority("ENVIRONMENTAL_OFFICE").withRecipient("Env").withSentAt(SENT)
			.withDueAt(SENT.plusWeeks(4)).withResponseText("ok").withStatus("SENT").withCreated(CREATED).withModified(CREATED);

		assertThat(entity.getId()).isEqualTo("r1");
		assertThat(entity.getErrandId()).isEqualTo("e1");
		assertThat(entity.getAuthority()).isEqualTo("ENVIRONMENTAL_OFFICE");
		assertThat(entity.getRecipient()).isEqualTo("Env");
		assertThat(entity.getSentAt()).isEqualTo(SENT);
		assertThat(entity.getDueAt()).isEqualTo(SENT.plusWeeks(4));
		assertThat(entity.getResponseText()).isEqualTo("ok");
		assertThat(entity.getStatus()).isEqualTo("SENT");
		assertThat(entity.getCreated()).isEqualTo(CREATED);
		assertThat(entity.getModified()).isEqualTo(CREATED);
	}

	@Test
	void setters() {
		final var entity = ReferralEntity.create();
		entity.setId("r1");
		entity.setErrandId("e1");
		entity.setAuthority("ENVIRONMENTAL_OFFICE");
		entity.setRecipient("Env");
		entity.setSentAt(SENT);
		entity.setDueAt(SENT.plusWeeks(4));
		entity.setStatus("RESPONDED");
		entity.setResponseText("ok");
		entity.setCreated(CREATED);
		entity.setModified(CREATED);

		assertThat(entity.getId()).isEqualTo("r1");
		assertThat(entity.getErrandId()).isEqualTo("e1");
		assertThat(entity.getAuthority()).isEqualTo("ENVIRONMENTAL_OFFICE");
		assertThat(entity.getRecipient()).isEqualTo("Env");
		assertThat(entity.getSentAt()).isEqualTo(SENT);
		assertThat(entity.getDueAt()).isEqualTo(SENT.plusWeeks(4));
		assertThat(entity.getStatus()).isEqualTo("RESPONDED");
		assertThat(entity.getResponseText()).isEqualTo("ok");
		assertThat(entity.getCreated()).isEqualTo(CREATED);
		assertThat(entity.getModified()).isEqualTo(CREATED);
	}

	@Test
	void toStringContainsValues() {
		final var entity = ReferralEntity.create().withId("r1").withErrandId("e1").withStatus("SENT");

		assertThat(entity.toString()).contains("ReferralEntity{", "id='r1'", "errandId='e1'", "status='SENT'");
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(ReferralEntity.create()).hasAllNullFieldsOrProperties();
		assertThat(new ReferralEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsAndHashCode() {
		final var a = ReferralEntity.create().withId("1").withErrandId("e").withStatus("SENT");
		final var b = ReferralEntity.create().withId("1").withErrandId("e").withStatus("SENT");
		final var c = ReferralEntity.create().withId("2");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b)
			.isNotEqualTo(c)
			.isNotEqualTo(null)
			.isNotEqualTo("string");
	}
}
