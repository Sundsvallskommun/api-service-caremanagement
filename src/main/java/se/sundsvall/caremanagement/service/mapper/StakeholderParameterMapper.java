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
			.map(e -> StakeholderParameter.create()
				.withId(e.getId())
				.withKey(e.getKey())
				.withDisplayName(e.getDisplayName())
				.withValues(ofNullable(e.getValues()).map(ArrayList::new).orElse(null)))
			.orElse(null);
	}

	public static StakeholderParameterEntity toStakeholderParameterEntity(final StakeholderParameter parameter, final StakeholderEntity stakeholderEntity) {
		return ofNullable(parameter)
			.map(p -> StakeholderParameterEntity.create()
				.withStakeholderEntity(stakeholderEntity)
				.withKey(p.getKey())
				.withDisplayName(p.getDisplayName())
				.withValues(ofNullable(p.getValues()).map(ArrayList::new).orElse(null)))
			.orElse(null);
	}

	public static StakeholderParameterEntity updateStakeholderParameterEntity(final StakeholderParameterEntity entity, final StakeholderParameter source) {
		if (entity == null || source == null) {
			return entity;
		}
		entity.setKey(source.getKey());
		entity.setDisplayName(source.getDisplayName());
		entity.setValues(ofNullable(source.getValues()).map(ArrayList::new).orElse(null));
		return entity;
	}

	public static List<StakeholderParameter> toStakeholderParameterList(final List<StakeholderParameterEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(StakeholderParameterMapper::toStakeholderParameter)
			.toList();
	}

	public static List<StakeholderParameterEntity> toStakeholderParameterEntityList(final List<StakeholderParameter> parameters, final StakeholderEntity stakeholderEntity) {
		return ofNullable(parameters).orElse(emptyList()).stream()
			.map(p -> toStakeholderParameterEntity(p, stakeholderEntity))
			.toList();
	}
}
