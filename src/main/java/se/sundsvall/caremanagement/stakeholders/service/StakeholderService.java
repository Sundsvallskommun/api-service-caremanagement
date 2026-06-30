package se.sundsvall.caremanagement.stakeholders.service;

import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.integration.db.StakeholderRepository;
import se.sundsvall.caremanagement.stakeholders.integration.db.model.StakeholderEntity;
import se.sundsvall.caremanagement.stakeholders.service.event.StakeholderMutated;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
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
	private static final String INVALID_ROLE_MESSAGE = "Role '%s' is not valid for errand type '%s'";

	private final ErrandRepository errandRepository;
	private final StakeholderRepository stakeholderRepository;
	private final StakeholderRoleRegistry roleRegistry;
	private final ApplicationEventPublisher eventPublisher;

	StakeholderService(final ErrandRepository errandRepository, final StakeholderRepository stakeholderRepository,
		final StakeholderRoleRegistry roleRegistry, final ApplicationEventPublisher eventPublisher) {
		this.errandRepository = errandRepository;
		this.stakeholderRepository = stakeholderRepository;
		this.roleRegistry = roleRegistry;
		this.eventPublisher = eventPublisher;
	}

	public String create(final String municipalityId, final String namespace, final String errandId, final Stakeholder stakeholder) {
		final var errand = ensureErrandExists(municipalityId, namespace, errandId);
		validateRole(errand.getTypeSlug(), stakeholder.getRole());
		final var saved = stakeholderRepository.save(toStakeholderEntity(stakeholder, errandId));
		eventPublisher.publishEvent(new StakeholderMutated(municipalityId, namespace, errandId));
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
		final var errand = ensureErrandExists(municipalityId, namespace, errandId);
		validateRole(errand.getTypeSlug(), stakeholder.getRole());
		final var entity = findStakeholder(municipalityId, namespace, errandId, stakeholderId);
		updateStakeholderEntity(entity, stakeholder);
		stakeholderRepository.save(entity);
		eventPublisher.publishEvent(new StakeholderMutated(municipalityId, namespace, errandId));
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String stakeholderId) {
		final var entity = findStakeholder(municipalityId, namespace, errandId, stakeholderId);
		stakeholderRepository.delete(entity);
		eventPublisher.publishEvent(new StakeholderMutated(municipalityId, namespace, errandId));
	}

	private ErrandEntity ensureErrandExists(final String municipalityId, final String namespace, final String errandId) {
		return errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private void validateRole(final String typeSlug, final String role) {
		if (roleRegistry.knownTypes().contains(typeSlug) && !roleRegistry.isValidRole(typeSlug, role)) {
			throw Problem.valueOf(BAD_REQUEST, INVALID_ROLE_MESSAGE.formatted(role, typeSlug));
		}
	}

	private StakeholderEntity findStakeholder(final String municipalityId, final String namespace, final String errandId, final String stakeholderId) {
		ensureErrandExists(municipalityId, namespace, errandId);
		return stakeholderRepository.findById(stakeholderId)
			.filter(entity -> errandId.equals(entity.getErrandId()))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, STAKEHOLDER_NOT_FOUND_MESSAGE.formatted(stakeholderId, errandId, namespace, municipalityId)));
	}
}
