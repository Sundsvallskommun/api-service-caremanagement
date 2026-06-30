package se.sundsvall.caremanagement.referral.service.mapper;

import java.util.List;
import se.sundsvall.caremanagement.referral.api.model.Referral;
import se.sundsvall.caremanagement.referral.integration.db.model.ReferralEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class ReferralMapper {

	private ReferralMapper() {}

	public static Referral toReferral(final ReferralEntity entity) {
		return ofNullable(entity)
			.map(e -> Referral.create()
				.withId(e.getId())
				.withAuthority(e.getAuthority())
				.withRecipient(e.getRecipient())
				.withSentAt(e.getSentAt())
				.withDueAt(e.getDueAt())
				.withResponseText(e.getResponseText())
				.withStatus(e.getStatus())
				.withCreated(e.getCreated())
				.withModified(e.getModified()))
			.orElse(null);
	}

	public static ReferralEntity toReferralEntity(final Referral referral, final String errandId) {
		return ofNullable(referral)
			.map(source -> ReferralEntity.create()
				.withErrandId(errandId)
				.withAuthority(source.getAuthority())
				.withRecipient(source.getRecipient())
				.withSentAt(source.getSentAt())
				.withDueAt(source.getDueAt())
				.withResponseText(source.getResponseText())
				.withStatus(source.getStatus()))
			.orElse(null);
	}

	public static List<Referral> toReferralList(final List<ReferralEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ReferralMapper::toReferral)
			.toList();
	}
}
