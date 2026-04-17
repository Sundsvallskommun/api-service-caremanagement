package se.sundsvall.caremanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import se.sundsvall.caremanagement.api.model.StakeholderParameter;
import se.sundsvall.caremanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.caremanagement.integration.db.model.StakeholderParameterEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class StakeholderParameterMapper {

	private StakeholderParameterMapper() {}

	public static StakeholderParameter toStakeholderParameter(final StakeholderParameterEntity entity) {
		return ofNullable(entity)
			.map(parameterEntity -> StakeholderParameter.create()
				.withId(parameterEntity.getId())
				.withKey(parameterEntity.getKey())
				.withDisplayName(parameterEntity.getDisplayName())
				.withValues(ofNullable(parameterEntity.getValues()).map(ArrayList::new).orElse(null)))
			.orElse(null);
	}

	public static StakeholderParameterEntity toStakeholderParameterEntity(final StakeholderParameter parameter, final StakeholderEntity stakeholderEntity) {
		return ofNullable(parameter)
			.map(source -> StakeholderParameterEntity.create()
				.withStakeholderEntity(stakeholderEntity)
				.withKey(source.getKey())
				.withDisplayName(source.getDisplayName())
				.withValues(ofNullable(source.getValues()).map(ArrayList::new).orElse(null)))
			.orElse(null);
	}

	/**
	 * Applies non-null fields from {@code source} onto {@code entity}. Null fields on the source mean
	 * "leave existing value untouched" (PATCH semantics).
	 */
	public static StakeholderParameterEntity updateStakeholderParameterEntity(final StakeholderParameterEntity entity, final StakeholderParameter source) {
		if (entity == null || source == null) {
			return entity;
		}
		ofNullable(source.getKey()).ifPresent(entity::setKey);
		ofNullable(source.getDisplayName()).ifPresent(entity::setDisplayName);
		ofNullable(source.getValues()).ifPresent(values -> entity.setValues(new ArrayList<>(values)));
		return entity;
	}

	public static List<StakeholderParameter> toStakeholderParameterList(final List<StakeholderParameterEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(StakeholderParameterMapper::toStakeholderParameter)
			.toList();
	}

	public static List<StakeholderParameterEntity> toStakeholderParameterEntityList(final List<StakeholderParameter> parameters, final StakeholderEntity stakeholderEntity) {
		return ofNullable(parameters).orElse(emptyList()).stream()
			.map(parameter -> toStakeholderParameterEntity(parameter, stakeholderEntity))
			.toList();
	}
}
