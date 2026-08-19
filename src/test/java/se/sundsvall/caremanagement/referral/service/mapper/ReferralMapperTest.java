package se.sundsvall.caremanagement.referral.service.mapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.referral.api.model.Referral;
import se.sundsvall.caremanagement.referral.integration.db.model.ReferralEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ReferralMapperTest {

	@Test
	void toReferralMapsAllFields() {
		final var entity = ReferralEntity.create()
			.withId("r1").withAuthority("ENVIRONMENTAL_OFFICE").withRecipient("Env Office").withSentAt(LocalDate.parse("2026-06-03"))
			.withDueAt(LocalDate.parse("2026-07-01")).withResponseText("ok").withStatus("RESPONDED")
			.withCreated(OffsetDateTime.parse("2026-06-03T10:00:00Z")).withModified(OffsetDateTime.parse("2026-06-04T10:00:00Z"));

		final var referral = ReferralMapper.toReferral(entity);

		assertThat(referral).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(referral.getId()).isEqualTo("r1");
		assertThat(referral.getAuthority()).isEqualTo("ENVIRONMENTAL_OFFICE");
		assertThat(referral.getRecipient()).isEqualTo("Env Office");
		assertThat(referral.getSentAt()).isEqualTo(LocalDate.parse("2026-06-03"));
		assertThat(referral.getDueAt()).isEqualTo(LocalDate.parse("2026-07-01"));
		assertThat(referral.getResponseText()).isEqualTo("ok");
		assertThat(referral.getStatus()).isEqualTo("RESPONDED");
		assertThat(referral.getCreated()).isEqualTo(OffsetDateTime.parse("2026-06-03T10:00:00Z"));
		assertThat(referral.getModified()).isEqualTo(OffsetDateTime.parse("2026-06-04T10:00:00Z"));
	}

	@Test
	void toReferralEntityMapsFieldsAndErrandId() {
		final var entity = ReferralMapper.toReferralEntity(
			Referral.create().withAuthority("POLICE").withRecipient("Local police").withStatus("SENT"), "errand-1");

		assertThat(entity).isNotNull()
			.hasNoNullFieldsOrPropertiesExcept("id", "sentAt", "dueAt", "responseText", "created", "modified");
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getAuthority()).isEqualTo("POLICE");
		assertThat(entity.getRecipient()).isEqualTo("Local police");
		assertThat(entity.getStatus()).isEqualTo("SENT");
	}

	@Test
	void nullSafe() {
		assertThat(ReferralMapper.toReferral(null)).isNull();
		assertThat(ReferralMapper.toReferralEntity(null, "errand-1")).isNull();
		assertThat(ReferralMapper.toReferralList(null)).isEmpty();
	}

	@Test
	void toReferralListMapsEach() {
		final var referrals = ReferralMapper.toReferralList(List.of(ReferralEntity.create().withId("r1"), ReferralEntity.create().withId("r2")));

		assertThat(referrals).extracting(Referral::getId).containsExactly("r1", "r2");
	}
}
