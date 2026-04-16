package se.sundsvall.caremanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import se.sundsvall.caremanagement.api.model.Parameter;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.ParameterEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class ParameterMapper {

	private ParameterMapper() {}

	public static Parameter toParameter(final ParameterEntity entity) {
		return ofNullable(entity)
			.map(parameterEntity -> Parameter.create()
				.withId(parameterEntity.getId())
				.withKey(parameterEntity.getKey())
				.withDisplayName(parameterEntity.getDisplayName())
				.withParameterGroup(parameterEntity.getParameterGroup())
				.withValues(ofNullable(parameterEntity.getValues()).map(ArrayList::new).orElse(null)))
			.orElse(null);
	}

	public static ParameterEntity toParameterEntity(final Parameter parameter, final ErrandEntity errandEntity) {
		return ofNullable(parameter)
			.map(source -> ParameterEntity.create()
				.withErrandEntity(errandEntity)
				.withKey(source.getKey())
				.withDisplayName(source.getDisplayName())
				.withParameterGroup(source.getParameterGroup())
				.withValues(ofNullable(source.getValues()).map(ArrayList::new).orElse(null)))
			.orElse(null);
	}

	/**
	 * Applies non-null fields from {@code source} onto {@code entity}. Null fields on the source mean
	 * "leave existing value untouched" (PATCH semantics).
	 */
	public static ParameterEntity updateParameterEntity(final ParameterEntity entity, final Parameter source) {
		if (entity == null || source == null) {
			return entity;
		}
		ofNullable(source.getKey()).ifPresent(entity::setKey);
		ofNullable(source.getDisplayName()).ifPresent(entity::setDisplayName);
		ofNullable(source.getParameterGroup()).ifPresent(entity::setParameterGroup);
		ofNullable(source.getValues()).ifPresent(values -> entity.setValues(new ArrayList<>(values)));
		return entity;
	}

	public static List<Parameter> toParameterList(final List<ParameterEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ParameterMapper::toParameter)
			.toList();
	}

	public static List<ParameterEntity> toParameterEntityList(final List<Parameter> parameters, final ErrandEntity errandEntity) {
		return ofNullable(parameters).orElse(emptyList()).stream()
			.map(parameter -> toParameterEntity(parameter, errandEntity))
			.toList();
	}
}
