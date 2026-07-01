package se.sundsvall.caremanagement.permit.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.permit.api.model.Permit;
import se.sundsvall.caremanagement.permit.integration.db.PermitRepository;
import se.sundsvall.caremanagement.permit.integration.db.model.PermitEntity;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.permit.service.mapper.PermitMapper.toPermit;
import static se.sundsvall.caremanagement.permit.service.mapper.PermitMapper.toPermitEntity;
import static se.sundsvall.caremanagement.permit.service.mapper.PermitMapper.toPermitList;

/**
 * Issues and manages permits on an errand. Type-agnostic and keyed by {@code errandId}: a decision on an errand can
 * issue a structured, time-bound, revocable permit that the flat {@code Decision} row cannot hold. On {@code issue},
 * {@code validFrom} defaults to today and {@code status} to ACTIVE; {@code validUntil} is whatever the caller supplied
 * (open-ended when omitted).
 */
@Service
@Transactional
public class PermitService {

	static final String STATUS_ACTIVE = "ACTIVE";
	static final String STATUS_REVOKED = "REVOKED";

	private static final String PERMIT_NOT_FOUND_MESSAGE = "No permit with id '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";

	private final ErrandAccessGuard errandGuard;
	private final PermitRepository permitRepository;

	PermitService(final ErrandAccessGuard errandGuard, final PermitRepository permitRepository) {
		this.errandGuard = errandGuard;
		this.permitRepository = permitRepository;
	}

	public String issue(final String municipalityId, final String namespace, final String errandId, final Permit permit) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		final var entity = toPermitEntity(permit, errandId);
		if (entity.getValidFrom() == null) {
			entity.setValidFrom(LocalDate.now(ZoneId.systemDefault()));
		}
		if (entity.getStatus() == null) {
			entity.setStatus(STATUS_ACTIVE);
		}
		return permitRepository.save(entity).getId();
	}

	@Transactional(readOnly = true)
	public Permit read(final String municipalityId, final String namespace, final String errandId, final String permitId) {
		return toPermit(findPermit(municipalityId, namespace, errandId, permitId));
	}

	@Transactional(readOnly = true)
	public List<Permit> readAll(final String municipalityId, final String namespace, final String errandId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		return toPermitList(permitRepository.findByErrandIdOrderByCreatedDesc(errandId));
	}

	/**
	 * Revokes a single permit — sets status to REVOKED.
	 */
	public void revoke(final String municipalityId, final String namespace, final String errandId, final String permitId) {
		final var entity = findPermit(municipalityId, namespace, errandId, permitId);
		entity.setStatus(STATUS_REVOKED);
		permitRepository.save(entity);
	}

	/**
	 * Revokes every permit on an errand that is not already REVOKED.
	 */
	public void revokeAllForErrand(final String municipalityId, final String namespace, final String errandId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		permitRepository.findByErrandIdOrderByCreatedDesc(errandId).stream()
			.filter(entity -> !STATUS_REVOKED.equals(entity.getStatus()))
			.forEach(entity -> {
				entity.setStatus(STATUS_REVOKED);
				permitRepository.save(entity);
			});
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String permitId) {
		final var entity = findPermit(municipalityId, namespace, errandId, permitId);
		permitRepository.delete(entity);
	}

	private PermitEntity findPermit(final String municipalityId, final String namespace, final String errandId, final String permitId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		return permitRepository.findByErrandIdAndId(errandId, permitId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, PERMIT_NOT_FOUND_MESSAGE.formatted(permitId, errandId, namespace, municipalityId)));
	}
}
