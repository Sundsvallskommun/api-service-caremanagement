package se.sundsvall.caremanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.api.model.Stakeholder;
import se.sundsvall.caremanagement.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.integration.db.StakeholderRepository;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.service.mapper.StakeholderMapper.toStakeholder;
import static se.sundsvall.caremanagement.service.mapper.StakeholderMapper.toStakeholderEntity;
import static se.sundsvall.caremanagement.service.mapper.StakeholderMapper.toStakeholderList;
import static se.sundsvall.caremanagement.service.mapper.StakeholderMapper.updateStakeholderEntity;

@Service
@Transactional
public class ErrandStakeholderService {

	private static final String ERRAND_NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String STAKEHOLDER_NOT_FOUND_MESSAGE = "No stakeholder with id '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";

	private final ErrandRepository errandRepository;
	private final StakeholderRepository stakeholderRepository;

	ErrandStakeholderService(final ErrandRepository errandRepository, final StakeholderRepository stakeholderRepository) {
		this.errandRepository = errandRepository;
		this.stakeholderRepository = stakeholderRepository;
	}

	public String create(final String municipalityId, final String namespace, final String errandId, final Stakeholder stakeholder) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		final var saved = stakeholderRepository.save(toStakeholderEntity(stakeholder, errand));
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public Stakeholder read(final String municipalityId, final String namespace, final String errandId, final String stakeholderId) {
		return toStakeholder(findStakeholder(municipalityId, namespace, errandId, stakeholderId));
	}

	@Transactional(readOnly = true)
	public List<Stakeholder> readAll(final String municipalityId, final String namespace, final String errandId) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		return toStakeholderList(errand.getStakeholders());
	}

	public void update(final String municipalityId, final String namespace, final String errandId, final String stakeholderId, final Stakeholder stakeholder) {
		final var entity = findStakeholder(municipalityId, namespace, errandId, stakeholderId);
		updateStakeholderEntity(entity, stakeholder);
		stakeholderRepository.save(entity);
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String stakeholderId) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		final var stakeholder = errand.getStakeholders().stream()
			.filter(entity -> entity.getId().equals(stakeholderId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, STAKEHOLDER_NOT_FOUND_MESSAGE.formatted(stakeholderId, errandId, namespace, municipalityId)));
		errand.getStakeholders().remove(stakeholder);
		errandRepository.save(errand);
	}

	private ErrandEntity findErrand(final String municipalityId, final String namespace, final String errandId) {
		return errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private StakeholderEntity findStakeholder(final String municipalityId, final String namespace, final String errandId, final String stakeholderId) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		return errand.getStakeholders().stream()
			.filter(entity -> entity.getId().equals(stakeholderId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, STAKEHOLDER_NOT_FOUND_MESSAGE.formatted(stakeholderId, errandId, namespace, municipalityId)));
	}
}
