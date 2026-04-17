package se.sundsvall.caremanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.api.model.Lookup;
import se.sundsvall.caremanagement.integration.db.LookupRepository;
import se.sundsvall.caremanagement.integration.db.model.LookupEntity;
import se.sundsvall.caremanagement.integration.db.model.LookupKind;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.service.mapper.LookupMapper.toLookup;
import static se.sundsvall.caremanagement.service.mapper.LookupMapper.toLookupEntity;
import static se.sundsvall.caremanagement.service.mapper.LookupMapper.toLookupList;
import static se.sundsvall.caremanagement.service.mapper.LookupMapper.updateLookupEntity;

@Service
@Transactional
public class MetadataService {

	private static final String NOT_FOUND_MESSAGE = "No %s with name '%s' found in namespace '%s' for municipality id '%s'";
	private static final String ALREADY_EXISTS_MESSAGE = "A %s with name '%s' already exists in namespace '%s' for municipality id '%s'";

	private final LookupRepository repository;

	MetadataService(final LookupRepository repository) {
		this.repository = repository;
	}

	public String create(final String municipalityId, final String namespace, final LookupKind kind, final Lookup lookup) {
		if (repository.existsByKindAndNamespaceAndMunicipalityIdAndName(kind, namespace, municipalityId, lookup.getName())) {
			throw Problem.valueOf(CONFLICT, ALREADY_EXISTS_MESSAGE.formatted(kindLabel(kind), lookup.getName(), namespace, municipalityId));
		}
		final var saved = repository.save(toLookupEntity(lookup, kind, namespace, municipalityId));
		return saved.getName();
	}

	public Lookup read(final String municipalityId, final String namespace, final LookupKind kind, final String name) {
		return toLookup(findEntity(municipalityId, namespace, kind, name));
	}

	public List<Lookup> readAll(final String municipalityId, final String namespace, final LookupKind kind) {
		return toLookupList(repository.findAllByKindAndNamespaceAndMunicipalityId(kind, namespace, municipalityId));
	}

	public void update(final String municipalityId, final String namespace, final LookupKind kind, final String name, final Lookup lookup) {
		final var entity = findEntity(municipalityId, namespace, kind, name);
		updateLookupEntity(entity, lookup);
		repository.save(entity);
	}

	public void delete(final String municipalityId, final String namespace, final LookupKind kind, final String name) {
		if (!repository.existsByKindAndNamespaceAndMunicipalityIdAndName(kind, namespace, municipalityId, name)) {
			throw Problem.valueOf(NOT_FOUND, NOT_FOUND_MESSAGE.formatted(kindLabel(kind), name, namespace, municipalityId));
		}
		repository.deleteByKindAndNamespaceAndMunicipalityIdAndName(kind, namespace, municipalityId, name);
	}

	private LookupEntity findEntity(final String municipalityId, final String namespace, final LookupKind kind, final String name) {
		return repository.findByKindAndNamespaceAndMunicipalityIdAndName(kind, namespace, municipalityId, name)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, NOT_FOUND_MESSAGE.formatted(kindLabel(kind), name, namespace, municipalityId)));
	}

	private static String kindLabel(final LookupKind kind) {
		return kind.name().toLowerCase().replace('_', ' ');
	}
}
