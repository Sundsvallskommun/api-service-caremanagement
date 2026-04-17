package se.sundsvall.caremanagement.service.mapper;

import se.sundsvall.caremanagement.api.model.NamespaceConfig;
import se.sundsvall.caremanagement.integration.db.model.NamespaceConfigEntity;

import static java.util.Optional.ofNullable;

public final class NamespaceConfigMapper {

	private NamespaceConfigMapper() {}

	public static NamespaceConfig toNamespaceConfig(final NamespaceConfigEntity entity) {
		return ofNullable(entity)
			.map(configEntity -> NamespaceConfig.create()
				.withId(configEntity.getId())
				.withDisplayName(configEntity.getDisplayName())
				.withShortCode(configEntity.getShortCode())
				.withCreated(configEntity.getCreated())
				.withModified(configEntity.getModified()))
			.orElse(null);
	}

	public static NamespaceConfigEntity toNamespaceConfigEntity(final NamespaceConfig config, final String namespace, final String municipalityId) {
		return ofNullable(config)
			.map(source -> NamespaceConfigEntity.create()
				.withNamespace(namespace)
				.withMunicipalityId(municipalityId)
				.withDisplayName(source.getDisplayName())
				.withShortCode(source.getShortCode()))
			.orElse(null);
	}

	/**
	 * Applies non-null fields from {@code source} onto {@code entity}. Null fields on the source mean
	 * "leave existing value untouched" (PATCH semantics).
	 */
	public static NamespaceConfigEntity updateNamespaceConfigEntity(final NamespaceConfigEntity entity, final NamespaceConfig source) {
		if (entity == null || source == null) {
			return entity;
		}
		ofNullable(source.getDisplayName()).ifPresent(entity::setDisplayName);
		ofNullable(source.getShortCode()).ifPresent(entity::setShortCode);
		return entity;
	}
}
