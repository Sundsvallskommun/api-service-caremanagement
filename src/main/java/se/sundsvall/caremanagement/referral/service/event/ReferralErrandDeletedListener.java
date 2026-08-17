package se.sundsvall.caremanagement.referral.service.event;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.core.service.event.ErrandDeleted;
import se.sundsvall.caremanagement.referral.integration.db.ReferralRepository;

/**
 * Removes every referral row tied to a deleted errand. Mirrors the decisions module's cleanup —
 * {@code @ApplicationModuleListener} runs asynchronously in a fresh transaction after the originating delete commits,
 * keeping the modules loosely coupled.
 */
@Component
class ReferralErrandDeletedListener {

	private final ReferralRepository referralRepository;

	ReferralErrandDeletedListener(final ReferralRepository referralRepository) {
		this.referralRepository = referralRepository;
	}

	@ApplicationModuleListener
	void deleteReferralsForErrand(final ErrandDeleted event) {
		referralRepository.deleteByErrandId(event.errandId());
	}
}
