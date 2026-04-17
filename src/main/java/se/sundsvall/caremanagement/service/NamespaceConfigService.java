package se.sundsvall.caremanagement.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.api.model.NamespaceConfig;
import se.sundsvall.caremanagement.integration.db.NamespaceConfigRepository;
import se.sundsvall.caremanagement.integration.db.model.NamespaceConfigEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.service.mapper.NamespaceConfigMapper.toNamespaceConfig;
import static se.sundsvall.caremanagement.service.mapper.NamespaceConfigMapper.toNamespaceConfigEntity;
import static se.sundsvall.caremanagement.service.mapper.NamespaceConfigMapper.updateNamespaceConfigEntity;

@Service
@Transactional
public class NamespaceConfigService {

	private static final String NOT_FOUND_MESSAGE = "No namespace config found for namespace '%s' and municipality id '%s'";
	private static final String ALREADY_EXISTS_MESSAGE = "A namespace config already exists for namespace '%s' and municipality id '%s'";

	private final NamespaceConfigRepository repository;

	NamespaceConfigService(final NamespaceConfigRepository repository) {
		this.repository = repository;
	}

	public Long create(final String municipalityId, final String namespace, final NamespaceConfig config) {
		if (repository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(CONFLICT, ALREADY_EXISTS_MESSAGE.formatted(namespace, municipalityId));
		}
		final var saved = repository.save(toNamespaceConfigEntity(config, namespace, municipalityId));
		return saved.getId();
	}

	public NamespaceConfig read(final String municipalityId, final String namespace) {
		return toNamespaceConfig(findEntity(municipalityId, namespace));
	}

	public void update(final String municipalityId, final String namespace, final NamespaceConfig config) {
		final var entity = findEntity(municipalityId, namespace);
		updateNamespaceConfigEntity(entity, config);
		repository.save(entity);
	}

	public void delete(final String municipalityId, final String namespace) {
		if (!repository.existsByNamespaceAndMunicipalityId(namespace, municipalityId)) {
			throw Problem.valueOf(NOT_FOUND, NOT_FOUND_MESSAGE.formatted(namespace, municipalityId));
		}
		repository.deleteByNamespaceAndMunicipalityId(namespace, municipalityId);
	}

	private NamespaceConfigEntity findEntity(final String municipalityId, final String namespace) {
		return repository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NOT_FOUND_MESSAGE.formatted(namespace, municipalityId)));
	}
}
