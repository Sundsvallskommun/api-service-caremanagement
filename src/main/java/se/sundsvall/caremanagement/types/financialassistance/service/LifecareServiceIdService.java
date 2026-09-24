package se.sundsvall.caremanagement.types.financialassistance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.lifecare.service.ActualisationService;
import se.sundsvall.caremanagement.types.financialassistance.integration.db.FinancialAssistanceRepository;

import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

/**
 * The errand's Lifecare insats id — the applicant's open financial-assistance service in Lifecare, the key
 * Lifecare's own case reads (journal, documents, reminders, jobbstimulans) take. Draken's BFF reads those live and
 * gets the key off the errand.
 *
 * <p>
 * Intake stores it from the insats the actualisation was linked to. An errand that had no open insats then — a
 * nyansökan, whose insats the caseworker opens in Lifecare afterwards — gets it filled in here, the first time the
 * errand is read after the insats exists, and never looked up again after that.
 * </p>
 *
 * <p>
 * Best-effort by design. The lookup reaches the citizen register and Lifecare; if either is down the errand must still
 * open, so a failure answers {@code null} and the next read tries again. It runs outside the errand read's (read-only)
 * transaction and outside any transaction of its own: a lookup that fails inside a transactional service marks the
 * surrounding transaction rollback-only, so catching the failure would still fail the commit — and with it the read
 * ({@code UnexpectedRollbackException}, a 500 on every errand read while the citizen register was down). The one
 * write, storing a resolved id, runs in the repository's own transaction.
 * </p>
 */
@Service
public class LifecareServiceIdService {

	private static final Logger LOG = LoggerFactory.getLogger(LifecareServiceIdService.class);

	private final FinancialAssistanceRepository financialAssistanceRepository;
	private final HouseholdPartyService householdPartyService;
	private final ActualisationService actualisationService;

	LifecareServiceIdService(final FinancialAssistanceRepository financialAssistanceRepository, final HouseholdPartyService householdPartyService,
		final ActualisationService actualisationService) {
		this.financialAssistanceRepository = financialAssistanceRepository;
		this.householdPartyService = householdPartyService;
		this.actualisationService = actualisationService;
	}

	/**
	 * The stored insats id, or — when the errand has none yet — the applicant's open EB insats looked up in Lifecare and
	 * stored.
	 *
	 * @return the insats id, or {@code null} when there is no errand data, no open EB insats, or the lookup failed
	 */
	@Transactional(propagation = NOT_SUPPORTED)
	public Integer currentOrResolve(final String municipalityId, final String namespace, final String errandId) {
		final var entity = financialAssistanceRepository.findByErrandId(errandId).orElse(null);
		if (entity == null) {
			return null;
		}
		if (entity.getLifecareServiceId() != null) {
			return entity.getLifecareServiceId();
		}

		try {
			final var resolved = householdPartyService.household(municipalityId, namespace, errandId).applicantPartyId()
				.filter(StringUtils::hasText)
				.flatMap(personId -> actualisationService.findFinancialAssistanceServiceId(municipalityId, personId));
			resolved.ifPresent(serviceId -> financialAssistanceRepository.save(entity.withLifecareServiceId(serviceId)));
			return resolved.orElse(null);
		} catch (final RuntimeException e) {
			// The exception type only: messages from the Lifecare lookup may carry the personal number.
			LOG.warn("Could not look up the Lifecare insats for errand {} ({}); the next read tries again", errandId, e.getClass().getSimpleName());
			return null;
		}
	}
}
