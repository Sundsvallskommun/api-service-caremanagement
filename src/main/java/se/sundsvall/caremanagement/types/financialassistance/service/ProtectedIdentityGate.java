package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.citizen.service.CitizenService;
import se.sundsvall.caremanagement.lifecare.service.LifecareCaseService;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * The protected-identity (skyddad identitet) gate of a <em>created</em> financial assistance errand. It answers one
 * question — may this errand go into automatic preparation, or must a caseworker take it by hand — and it answers it
 * <b>fail closed</b>: a party who is flagged in the population register (citizen) or in Lifecare FamilyCare counts as
 * protected, and so does a party whose flag could not be read, because an unknown answer is not a clearance.
 *
 * <p>
 * This is deliberately the opposite of the best-effort check in {@link EligibilityService}. There the same lookups
 * only steer which application the citizen is offered, so an outage degrades to normal routing rather than shutting
 * every applicant out. Here the outcome is what the errand is allowed to trigger (the Operaton process, the SSBTEK
 * read, the Lifecare actualisation and the normberäkning proposal), so an outage must hold the errand back instead.
 *
 * <p>
 * The protected status never leaves this class as anything but a boolean: it is not written on the errand, exposed in
 * any API model, or logged beside a partyId, personal identity number or name. A lookup that fails is logged (errand
 * id and the failing source only) because the failure is an operational fact; a party that <em>is</em> protected is
 * not logged at all.
 */
@Component
public class ProtectedIdentityGate {

	private static final Logger LOG = LoggerFactory.getLogger(ProtectedIdentityGate.class);

	private final CitizenService citizenService;
	private final LifecareCaseService lifecareCaseService;

	public ProtectedIdentityGate(final CitizenService citizenService, final LifecareCaseService lifecareCaseService) {
		this.citizenService = citizenService;
		this.lifecareCaseService = lifecareCaseService;
	}

	/**
	 * Whether any of the parties has protected identity, or the answer could not be established for one of them.
	 * Blank partyIds are ignored, and the check stops at the first party (and the first source) that is protected or
	 * unknown — the outcome cannot change after that, so there is no reason to read more identities than necessary.
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  errandId       the errand the check is made for; only used to attribute the log line of a failed lookup
	 * @param  partyIds       the partyIds of everyone on the application (applicant, co-applicant, children)
	 * @return                {@code true} when a party is protected or a lookup failed; {@code false} when every party
	 *                        was read successfully in both sources and none is protected (also when there is no
	 *                        party to check)
	 */
	public boolean protectedOrUnknown(final String municipalityId, final String errandId, final Collection<String> partyIds) {
		return partyIds.stream()
			.filter(StringUtils::hasText)
			.distinct()
			.anyMatch(partyId -> citizenProtectedOrUnknown(municipalityId, errandId, partyId)
				|| lifecareProtectedOrUnknown(municipalityId, errandId, partyId));
	}

	private boolean citizenProtectedOrUnknown(final String municipalityId, final String errandId, final String partyId) {
		try {
			return citizenService.hasProtectedIdentity(municipalityId, partyId);
		} catch (final RuntimeException e) {
			logUnknown("citizen", errandId, e);
			return true;
		}
	}

	private boolean lifecareProtectedOrUnknown(final String municipalityId, final String errandId, final String partyId) {
		try {
			return lifecareCaseService.hasProtectedIdentity(municipalityId, partyId);
		} catch (final RuntimeException e) {
			logUnknown("Lifecare", errandId, e);
			return true;
		}
	}

	/**
	 * One WARN per failed lookup: the errand, the source and the exception class. Never the exception itself or its
	 * message — the integrations put the partyId or the personal identity number in both (a Feign transport failure
	 * carries the request URL, a not-found problem carries the partyId).
	 */
	private static void logUnknown(final String source, final String errandId, final RuntimeException e) {
		LOG.warn("Could not check protected identity in {} for financial assistance errand {} ({}); the errand is held for manual review",
			source, sanitizeForLogging(errandId), e.getClass().getSimpleName());
	}
}
