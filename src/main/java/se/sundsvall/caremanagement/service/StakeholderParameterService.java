package se.sundsvall.caremanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.api.model.StakeholderParameter;
import se.sundsvall.caremanagement.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.integration.db.StakeholderParameterRepository;
import se.sundsvall.caremanagement.integration.db.StakeholderRepository;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.caremanagement.integration.db.model.StakeholderParameterEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.service.mapper.StakeholderParameterMapper.toStakeholderParameter;
import static se.sundsvall.caremanagement.service.mapper.StakeholderParameterMapper.toStakeholderParameterEntity;
import static se.sundsvall.caremanagement.service.mapper.StakeholderParameterMapper.toStakeholderParameterList;
import static se.sundsvall.caremanagement.service.mapper.StakeholderParameterMapper.updateStakeholderParameterEntity;

@Service
@Transactional
public class StakeholderParameterService {

	private static final String ERRAND_NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String STAKEHOLDER_NOT_FOUND_MESSAGE = "No stakeholder with id '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";
	private static final String PARAMETER_NOT_FOUND_MESSAGE = "No parameter with id '%s' found on stakeholder '%s' in namespace '%s' for municipality id '%s'";

	private final ErrandRepository errandRepository;
	private final StakeholderRepository stakeholderRepository;
	private final StakeholderParameterRepository parameterRepository;

	StakeholderParameterService(
		final ErrandRepository errandRepository,
		final StakeholderRepository stakeholderRepository,
		final StakeholderParameterRepository parameterRepository) {
		this.errandRepository = errandRepository;
		this.stakeholderRepository = stakeholderRepository;
		this.parameterRepository = parameterRepository;
	}

	public Long create(final String municipalityId, final String namespace, final String errandId, final String stakeholderId, final StakeholderParameter parameter) {
		final var stakeholder = findStakeholder(municipalityId, namespace, errandId, stakeholderId);
		final var saved = parameterRepository.save(toStakeholderParameterEntity(parameter, stakeholder));
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public StakeholderParameter read(final String municipalityId, final String namespace, final String errandId, final String stakeholderId, final Long parameterId) {
		return toStakeholderParameter(findParameter(municipalityId, namespace, errandId, stakeholderId, parameterId));
	}

	@Transactional(readOnly = true)
	public List<StakeholderParameter> readAll(final String municipalityId, final String namespace, final String errandId, final String stakeholderId) {
		final var stakeholder = findStakeholder(municipalityId, namespace, errandId, stakeholderId);
		return toStakeholderParameterList(stakeholder.getParameters());
	}

	public void update(final String municipalityId, final String namespace, final String errandId, final String stakeholderId, final Long parameterId, final StakeholderParameter parameter) {
		final var entity = findParameter(municipalityId, namespace, errandId, stakeholderId, parameterId);
		updateStakeholderParameterEntity(entity, parameter);
		parameterRepository.save(entity);
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String stakeholderId, final Long parameterId) {
		final var stakeholder = findStakeholder(municipalityId, namespace, errandId, stakeholderId);
		final var parameter = stakeholder.getParameters().stream()
			.filter(entity -> entity.getId() == parameterId)
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, PARAMETER_NOT_FOUND_MESSAGE.formatted(parameterId, stakeholderId, namespace, municipalityId)));
		stakeholder.getParameters().remove(parameter);
		stakeholderRepository.save(stakeholder);
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

	private StakeholderParameterEntity findParameter(final String municipalityId, final String namespace, final String errandId, final String stakeholderId, final Long parameterId) {
		final var stakeholder = findStakeholder(municipalityId, namespace, errandId, stakeholderId);
		return stakeholder.getParameters().stream()
			.filter(entity -> entity.getId() == parameterId)
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, PARAMETER_NOT_FOUND_MESSAGE.formatted(parameterId, stakeholderId, namespace, municipalityId)));
	}
}
