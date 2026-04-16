package se.sundsvall.caremanagement.service.mapper;

import se.sundsvall.caremanagement.api.model.NamespaceConfig;
import se.sundsvall.caremanagement.integration.db.model.NamespaceConfigEntity;

import static java.util.Optional.ofNullable;

public final class NamespaceConfigMapper {

	private NamespaceConfigMapper() {}

	public static NamespaceConfig toNamespaceConfig(final NamespaceConfigEntity entity) {
		return ofNullable(entity)
			.map(e -> NamespaceConfig.create()
				.withId(e.getId())
				.withDisplayName(e.getDisplayName())
				.withShortCode(e.getShortCode())
				.withCreated(e.getCreated())
				.withModified(e.getModified()))
			.orElse(null);
	}

	public static NamespaceConfigEntity toNamespaceConfigEntity(final NamespaceConfig config, final String namespace, final String municipalityId) {
		return ofNullable(config)
			.map(c -> NamespaceConfigEntity.create()
				.withNamespace(namespace)
				.withMunicipalityId(municipalityId)
				.withDisplayName(c.getDisplayName())
				.withShortCode(c.getShortCode()))
			.orElse(null);
	}

	public static NamespaceConfigEntity updateNamespaceConfigEntity(final NamespaceConfigEntity entity, final NamespaceConfig source) {
		if (entity == null || source == null) {
			return entity;
		}
		entity.setDisplayName(source.getDisplayName());
		entity.setShortCode(source.getShortCode());
		return entity;
	}
}
