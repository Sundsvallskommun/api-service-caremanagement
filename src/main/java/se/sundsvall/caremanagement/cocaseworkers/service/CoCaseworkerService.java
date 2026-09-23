package se.sundsvall.caremanagement.cocaseworkers.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.cocaseworkers.api.model.AddCoCaseworker;
import se.sundsvall.caremanagement.cocaseworkers.api.model.CoCaseworker;
import se.sundsvall.caremanagement.cocaseworkers.integration.db.CoCaseworkerRepository;
import se.sundsvall.caremanagement.cocaseworkers.integration.db.model.CoCaseworkerEntity;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.Problem;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class CoCaseworkerService {

	private static final String ALREADY_ADDED_MESSAGE = "User '%s' is already a co-caseworker on errand '%s'";
	private static final String NOT_FOUND_MESSAGE = "No co-caseworker '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";

	private final ErrandAccessGuard errandGuard;
	private final CoCaseworkerRepository coCaseworkerRepository;

	CoCaseworkerService(final ErrandAccessGuard errandGuard, final CoCaseworkerRepository coCaseworkerRepository) {
		this.errandGuard = errandGuard;
		this.coCaseworkerRepository = coCaseworkerRepository;
	}

	public String add(final String municipalityId, final String namespace, final String errandId, final AddCoCaseworker request) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		if (coCaseworkerRepository.existsByNamespaceAndMunicipalityIdAndErrandIdAndUserId(namespace, municipalityId, errandId, request.userId())) {
			throw Problem.valueOf(CONFLICT, ALREADY_ADDED_MESSAGE.formatted(request.userId(), errandId));
		}

		final var entity = CoCaseworkerEntity.create()
			.withErrandId(errandId)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withUserId(request.userId())
			.withCreated(now(systemDefault()).truncatedTo(MILLIS));

		return coCaseworkerRepository.save(entity).getId();
	}

	@Transactional(readOnly = true)
	public List<CoCaseworker> listForErrand(final String municipalityId, final String namespace, final String errandId, final Sort sort) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		return coCaseworkerRepository.findAllByNamespaceAndMunicipalityIdAndErrandId(namespace, municipalityId, errandId, sort).stream()
			.map(CoCaseworkerService::toCoCaseworker)
			.toList();
	}

	public void remove(final String municipalityId, final String namespace, final String errandId, final String userId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		final var entity = coCaseworkerRepository.findByNamespaceAndMunicipalityIdAndErrandIdAndUserId(namespace, municipalityId, errandId, userId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NOT_FOUND_MESSAGE.formatted(userId, errandId, namespace, municipalityId)));

		coCaseworkerRepository.delete(entity);
	}

	private static CoCaseworker toCoCaseworker(final CoCaseworkerEntity e) {
		return CoCaseworker.create()
			.withId(e.getId())
			.withErrandId(e.getErrandId())
			.withUserId(e.getUserId())
			.withCreated(e.getCreated());
	}
}
