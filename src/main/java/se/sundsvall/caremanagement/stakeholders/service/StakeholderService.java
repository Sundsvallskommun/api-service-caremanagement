package se.sundsvall.caremanagement.stakeholders.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.integration.db.StakeholderRepository;
import se.sundsvall.caremanagement.stakeholders.integration.db.model.StakeholderEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.stakeholders.service.mapper.StakeholderMapper.toStakeholder;
import static se.sundsvall.caremanagement.stakeholders.service.mapper.StakeholderMapper.toStakeholderEntity;
import static se.sundsvall.caremanagement.stakeholders.service.mapper.StakeholderMapper.toStakeholderList;
import static se.sundsvall.caremanagement.stakeholders.service.mapper.StakeholderMapper.updateStakeholderEntity;

@Service
@Transactional
public class StakeholderService {

	private static final String ERRAND_NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String STAKEHOLDER_NOT_FOUND_MESSAGE = "No stakeholder with id '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";

	private final ErrandRepository errandRepository;
	private final StakeholderRepository stakeholderRepository;

	StakeholderService(final ErrandRepository errandRepository, final StakeholderRepository stakeholderRepository) {
		this.errandRepository = errandRepository;
		this.stakeholderRepository = stakeholderRepository;
	}

	public String create(final String municipalityId, final String namespace, final String errandId, final Stakeholder stakeholder) {
		ensureErrandExists(municipalityId, namespace, errandId);
		final var saved = stakeholderRepository.save(toStakeholderEntity(stakeholder, errandId));
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public Stakeholder read(final String municipalityId, final String namespace, final String errandId, final String stakeholderId) {
		return toStakeholder(findStakeholder(municipalityId, namespace, errandId, stakeholderId));
	}

	@Transactional(readOnly = true)
	public List<Stakeholder> readAll(final String municipalityId, final String namespace, final String errandId) {
		ensureErrandExists(municipalityId, namespace, errandId);
		return toStakeholderList(stakeholderRepository.findByErrandId(errandId));
	}

	public void update(final String municipalityId, final String namespace, final String errandId, final String stakeholderId, final Stakeholder stakeholder) {
		final var entity = findStakeholder(municipalityId, namespace, errandId, stakeholderId);
		updateStakeholderEntity(entity, stakeholder);
		stakeholderRepository.save(entity);
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String stakeholderId) {
		final var entity = findStakeholder(municipalityId, namespace, errandId, stakeholderId);
		stakeholderRepository.delete(entity);
	}

	private void ensureErrandExists(final String municipalityId, final String namespace, final String errandId) {
		errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private StakeholderEntity findStakeholder(final String municipalityId, final String namespace, final String errandId, final String stakeholderId) {
		ensureErrandExists(municipalityId, namespace, errandId);
		return stakeholderRepository.findById(stakeholderId)
			.filter(entity -> errandId.equals(entity.getErrandId()))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, STAKEHOLDER_NOT_FOUND_MESSAGE.formatted(stakeholderId, errandId, namespace, municipalityId)));
	}
}
