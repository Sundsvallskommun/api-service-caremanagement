package se.sundsvall.caremanagement.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.api.model.Errand;
import se.sundsvall.caremanagement.api.model.FindErrandsResponse;
import se.sundsvall.caremanagement.api.model.PatchErrand;
import se.sundsvall.caremanagement.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.integration.db.LookupRepository;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.LookupEntity;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.integration.db.model.LookupKind.CONTACT_REASON;
import static se.sundsvall.caremanagement.integration.db.specification.ErrandSpecification.withNamespaceAndMunicipalityId;
import static se.sundsvall.caremanagement.service.mapper.ErrandMapper.toErrand;
import static se.sundsvall.caremanagement.service.mapper.ErrandMapper.toErrandEntity;
import static se.sundsvall.caremanagement.service.mapper.ErrandMapper.toFindErrandsResponse;
import static se.sundsvall.caremanagement.service.mapper.PatchMapper.patchErrand;

@Service
@Transactional
public class ErrandService {

	private static final String NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String CONTACT_REASON_NOT_FOUND_MESSAGE = "No contact reason with name '%s' found in namespace '%s' for municipality id '%s'";

	private final ErrandRepository errandRepository;
	private final LookupRepository lookupRepository;
	private final ProcessService processService;

	ErrandService(final ErrandRepository errandRepository, final LookupRepository lookupRepository, final ProcessService processService) {
		this.errandRepository = errandRepository;
		this.lookupRepository = lookupRepository;
		this.processService = processService;
	}

	public String createErrand(final String municipalityId, final String namespace, final Errand errand) {
		final var contactReason = resolveContactReason(municipalityId, namespace, errand.getContactReason());
		final var saved = errandRepository.save(toErrandEntity(errand, namespace, municipalityId, contactReason));

		processService.startProcess(municipalityId, errand.getProcessDefinitionName(), saved.getId(), errand.getParameters())
			.ifPresent(instanceId -> {
				saved.setProcessInstanceId(instanceId);
				errandRepository.save(saved);
			});

		return saved.getId();
	}

	public Errand readErrand(final String municipalityId, final String namespace, final String errandId) {
		return toErrand(findEntity(municipalityId, namespace, errandId));
	}

	@Transactional(readOnly = true)
	public FindErrandsResponse findErrands(final String municipalityId, final String namespace, final Specification<ErrandEntity> filter, final Pageable pageable) {
		final var baseSpec = withNamespaceAndMunicipalityId(namespace, municipalityId);
		final var combined = ofNullable(filter).map(baseSpec::and).orElse(baseSpec);
		return toFindErrandsResponse(errandRepository.findAll(combined, pageable));
	}

	public void updateErrand(final String municipalityId, final String namespace, final String errandId, final PatchErrand patch) {
		final var entity = findEntity(municipalityId, namespace, errandId);
		patchErrand(entity, patch, resolveContactReason(municipalityId, namespace, patch.getContactReason()));
		errandRepository.save(entity);
	}

	public void deleteErrand(final String municipalityId, final String namespace, final String errandId) {
		final var entity = findEntity(municipalityId, namespace, errandId);
		errandRepository.delete(entity);
	}

	private ErrandEntity findEntity(final String municipalityId, final String namespace, final String errandId) {
		return errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private LookupEntity resolveContactReason(final String municipalityId, final String namespace, final String contactReasonName) {
		return ofNullable(contactReasonName)
			.map(name -> lookupRepository.findByKindAndNamespaceAndMunicipalityIdAndName(CONTACT_REASON, namespace, municipalityId, name)
				.orElseThrow(() -> Problem.valueOf(BAD_REQUEST, CONTACT_REASON_NOT_FOUND_MESSAGE.formatted(name, namespace, municipalityId))))
			.orElse(null);
	}
}
