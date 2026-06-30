package se.sundsvall.caremanagement.referral.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.referral.api.model.Referral;
import se.sundsvall.caremanagement.referral.integration.db.ReferralRepository;
import se.sundsvall.caremanagement.referral.integration.db.model.ReferralEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.referral.service.mapper.ReferralMapper.toReferral;
import static se.sundsvall.caremanagement.referral.service.mapper.ReferralMapper.toReferralEntity;
import static se.sundsvall.caremanagement.referral.service.mapper.ReferralMapper.toReferralList;

/**
 * Sends and manages referrals/consultations on an errand. Type-agnostic and keyed by {@code errandId}: an errand can be
 * referred to one or more external authorities for their response. On {@code create}, {@code sentAt} defaults to today
 * and {@code status} to SENT; {@code registerResponse} stores the response and flips status to RESPONDED.
 */
@Service
@Transactional
public class ReferralService {

	static final String STATUS_SENT = "SENT";
	static final String STATUS_RESPONDED = "RESPONDED";

	private static final String ERRAND_NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String REFERRAL_NOT_FOUND_MESSAGE = "No referral with id '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";

	private final ErrandRepository errandRepository;
	private final ReferralRepository referralRepository;

	ReferralService(final ErrandRepository errandRepository, final ReferralRepository referralRepository) {
		this.errandRepository = errandRepository;
		this.referralRepository = referralRepository;
	}

	public String create(final String municipalityId, final String namespace, final String errandId, final Referral referral) {
		ensureErrandExists(municipalityId, namespace, errandId);
		final var entity = toReferralEntity(referral, errandId);
		if (entity.getSentAt() == null) {
			entity.setSentAt(LocalDate.now(ZoneId.systemDefault()));
		}
		if (entity.getStatus() == null) {
			entity.setStatus(STATUS_SENT);
		}
		return referralRepository.save(entity).getId();
	}

	@Transactional(readOnly = true)
	public Referral read(final String municipalityId, final String namespace, final String errandId, final String referralId) {
		return toReferral(findReferral(municipalityId, namespace, errandId, referralId));
	}

	@Transactional(readOnly = true)
	public List<Referral> readAll(final String municipalityId, final String namespace, final String errandId) {
		ensureErrandExists(municipalityId, namespace, errandId);
		return toReferralList(referralRepository.findByErrandIdOrderByCreatedDesc(errandId));
	}

	/**
	 * Registers a response on a referral — stores the text and sets status to RESPONDED.
	 */
	public void registerResponse(final String municipalityId, final String namespace, final String errandId, final String referralId, final String responseText) {
		final var entity = findReferral(municipalityId, namespace, errandId, referralId);
		entity.setResponseText(responseText);
		entity.setStatus(STATUS_RESPONDED);
		referralRepository.save(entity);
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String referralId) {
		final var entity = findReferral(municipalityId, namespace, errandId, referralId);
		referralRepository.delete(entity);
	}

	private void ensureErrandExists(final String municipalityId, final String namespace, final String errandId) {
		errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private ReferralEntity findReferral(final String municipalityId, final String namespace, final String errandId, final String referralId) {
		ensureErrandExists(municipalityId, namespace, errandId);
		return referralRepository.findByErrandIdAndId(errandId, referralId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, REFERRAL_NOT_FOUND_MESSAGE.formatted(referralId, errandId, namespace, municipalityId)));
	}
}
