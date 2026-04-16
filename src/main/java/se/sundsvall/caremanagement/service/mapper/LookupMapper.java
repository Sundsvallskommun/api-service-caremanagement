package se.sundsvall.caremanagement.service.mapper;

import java.util.List;
import se.sundsvall.caremanagement.api.model.Lookup;
import se.sundsvall.caremanagement.integration.db.model.LookupEntity;
import se.sundsvall.caremanagement.integration.db.model.LookupKind;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class LookupMapper {

	private LookupMapper() {}

	public static Lookup toLookup(final LookupEntity entity) {
		return ofNullable(entity)
			.map(e -> Lookup.create()
				.withName(e.getName())
				.withDisplayName(e.getDisplayName())
				.withCreated(e.getCreated())
				.withModified(e.getModified()))
			.orElse(null);
	}

	public static LookupEntity toLookupEntity(final Lookup lookup, final LookupKind kind, final String namespace, final String municipalityId) {
		return ofNullable(lookup)
			.map(l -> LookupEntity.create()
				.withKind(kind)
				.withName(l.getName())
				.withDisplayName(l.getDisplayName())
				.withNamespace(namespace)
				.withMunicipalityId(municipalityId))
			.orElse(null);
	}

	/**
	 * Applies updatable fields from {@code source} onto the existing entity. {@code name} is identity and is not
	 * touched; only {@code displayName} is mutable.
	 */
	public static LookupEntity updateLookupEntity(final LookupEntity entity, final Lookup source) {
		if (entity == null || source == null) {
			return entity;
		}
		entity.setDisplayName(source.getDisplayName());
		return entity;
	}

	public static List<Lookup> toLookupList(final List<LookupEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(LookupMapper::toLookup)
			.toList();
	}
}
