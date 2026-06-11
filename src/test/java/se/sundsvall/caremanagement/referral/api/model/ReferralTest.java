package se.sundsvall.caremanagement.referral.api.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferralTest {
	private static final LocalDate SENT = LocalDate.parse("2026-06-03");
	private static final LocalDate DUE = LocalDate.parse("2026-07-01");
	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-06-03T10:00:00Z");

	@Test
	void builderMethods() {
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
	void setters() {
		final var referral = Referral.create();
		referral.setId("r1");
		referral.setAuthority("POLICE");
		referral.setRecipient("Local police");
		referral.setSentAt(SENT);
		referral.setDueAt(DUE);
		referral.setResponseText("ok");
		referral.setStatus("SENT");
		referral.setCreated(CREATED);
		referral.setModified(CREATED);

		assertThat(referral.getAuthority()).isEqualTo("POLICE");
		assertThat(referral.getStatus()).isEqualTo("SENT");
		assertThat(referral.getDueAt()).isEqualTo(DUE);
	}

	@Test
	void createReturnsBlankInstance() {
		assertThat(Referral.create()).hasAllNullFieldsOrProperties();
	}

	@Test
	void equalsAndHashCode() {
		final var a = Referral.create().withId("1").withAuthority("A").withStatus("SENT");
		final var b = Referral.create().withId("1").withAuthority("A").withStatus("SENT");
		final var c = Referral.create().withId("2");

		assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(null);
		assertThat(a).isNotEqualTo("string");
	}
}
