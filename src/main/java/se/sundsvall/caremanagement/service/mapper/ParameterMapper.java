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
			.map(e -> Parameter.create()
				.withId(e.getId())
				.withKey(e.getKey())
				.withDisplayName(e.getDisplayName())
				.withParameterGroup(e.getParameterGroup())
				.withValues(ofNullable(e.getValues()).map(ArrayList::new).orElse(null)))
			.orElse(null);
	}

	public static ParameterEntity toParameterEntity(final Parameter parameter, final ErrandEntity errandEntity) {
		return ofNullable(parameter)
			.map(p -> ParameterEntity.create()
				.withErrandEntity(errandEntity)
				.withKey(p.getKey())
				.withDisplayName(p.getDisplayName())
				.withParameterGroup(p.getParameterGroup())
				.withValues(ofNullable(p.getValues()).map(ArrayList::new).orElse(null)))
			.orElse(null);
	}

	public static ParameterEntity updateParameterEntity(final ParameterEntity entity, final Parameter source) {
		if (entity == null || source == null) {
			return entity;
		}
		entity.setKey(source.getKey());
		entity.setDisplayName(source.getDisplayName());
		entity.setParameterGroup(source.getParameterGroup());
		entity.setValues(ofNullable(source.getValues()).map(ArrayList::new).orElse(null));
		return entity;
	}

	public static List<Parameter> toParameterList(final List<ParameterEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ParameterMapper::toParameter)
			.toList();
	}

	public static List<ParameterEntity> toParameterEntityList(final List<Parameter> parameters, final ErrandEntity errandEntity) {
		return ofNullable(parameters).orElse(emptyList()).stream()
			.map(p -> toParameterEntity(p, errandEntity))
			.toList();
	}
}
