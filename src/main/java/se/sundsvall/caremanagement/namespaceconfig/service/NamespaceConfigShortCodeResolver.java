package se.sundsvall.caremanagement.namespaceconfig.service;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.service.ErrandNumberPrefixResolver;
import se.sundsvall.caremanagement.namespaceconfig.integration.db.NamespaceConfigRepository;
import se.sundsvall.caremanagement.namespaceconfig.integration.db.model.NamespaceConfigEntity;

/**
 * Supplies the errand-number prefix from the namespace's configured short code (e.g. {@code EB}). Lives in the
 * {@code namespaceconfig} module — which owns the short code — and fulfils the {@link ErrandNumberPrefixResolver}
 * contract owned by {@code core}, so core never reaches into namespace configuration directly.
 */
@Component
class NamespaceConfigShortCodeResolver implements ErrandNumberPrefixResolver {

	private final NamespaceConfigRepository namespaceConfigRepository;

	NamespaceConfigShortCodeResolver(final NamespaceConfigRepository namespaceConfigRepository) {
		this.namespaceConfigRepository = namespaceConfigRepository;
	}

	@Override
	public Optional<String> resolvePrefix(final String municipalityId, final String namespace) {
		return namespaceConfigRepository.findByNamespaceAndMunicipalityId(namespace, municipalityId)
			.map(NamespaceConfigEntity::getShortCode)
			.filter(StringUtils::hasText);
	}
}
