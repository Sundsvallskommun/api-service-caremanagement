package se.sundsvall.caremanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import se.sundsvall.caremanagement.api.model.Stakeholder;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.StakeholderEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.service.mapper.ContactChannelMapper.toContactChannelList;
import static se.sundsvall.caremanagement.service.mapper.ContactChannelMapper.toTagEmbeddableList;
import static se.sundsvall.caremanagement.service.mapper.StakeholderParameterMapper.toStakeholderParameterEntityList;
import static se.sundsvall.caremanagement.service.mapper.StakeholderParameterMapper.toStakeholderParameterList;

public final class StakeholderMapper {

	private StakeholderMapper() {}

	public static Stakeholder toStakeholder(final StakeholderEntity entity) {
		return ofNullable(entity)
			.map(stakeholderEntity -> Stakeholder.create()
				.withId(stakeholderEntity.getId())
				.withExternalId(stakeholderEntity.getExternalId())
				.withExternalIdType(stakeholderEntity.getExternalIdType())
				.withRole(stakeholderEntity.getRole())
				.withFirstName(stakeholderEntity.getFirstName())
				.withLastName(stakeholderEntity.getLastName())
				.withOrganizationName(stakeholderEntity.getOrganizationName())
				.withAddress(stakeholderEntity.getAddress())
				.withCareOf(stakeholderEntity.getCareOf())
				.withZipCode(stakeholderEntity.getZipCode())
				.withCity(stakeholderEntity.getCity())
				.withCountry(stakeholderEntity.getCountry())
				.withContactChannels(toContactChannelList(stakeholderEntity.getContactChannels()))
				.withParameters(toStakeholderParameterList(stakeholderEntity.getParameters())))
			.orElse(null);
	}

	public static StakeholderEntity toStakeholderEntity(final Stakeholder stakeholder, final ErrandEntity errandEntity) {
		if (stakeholder == null) {
			return null;
		}
		final var stakeholderEntity = StakeholderEntity.create()
			.withErrandEntity(errandEntity)
			.withExternalId(stakeholder.getExternalId())
			.withExternalIdType(stakeholder.getExternalIdType())
			.withRole(stakeholder.getRole())
			.withFirstName(stakeholder.getFirstName())
			.withLastName(stakeholder.getLastName())
			.withOrganizationName(stakeholder.getOrganizationName())
			.withAddress(stakeholder.getAddress())
			.withCareOf(stakeholder.getCareOf())
			.withZipCode(stakeholder.getZipCode())
			.withCity(stakeholder.getCity())
			.withCountry(stakeholder.getCountry())
			.withContactChannels(new ArrayList<>(toTagEmbeddableList(stakeholder.getContactChannels())));
		return stakeholderEntity.withParameters(new ArrayList<>(toStakeholderParameterEntityList(stakeholder.getParameters(), stakeholderEntity)));
	}

	/**
	 * Applies non-null fields from {@code source} onto {@code entity}. Null fields on the source mean
	 * "leave existing value untouched" (PATCH semantics).
	 */
	public static StakeholderEntity updateStakeholderEntity(final StakeholderEntity entity, final Stakeholder source) {
		if (entity == null || source == null) {
			return entity;
		}
		ofNullable(source.getExternalId()).ifPresent(entity::setExternalId);
		ofNullable(source.getExternalIdType()).ifPresent(entity::setExternalIdType);
		ofNullable(source.getRole()).ifPresent(entity::setRole);
		ofNullable(source.getFirstName()).ifPresent(entity::setFirstName);
		ofNullable(source.getLastName()).ifPresent(entity::setLastName);
		ofNullable(source.getOrganizationName()).ifPresent(entity::setOrganizationName);
		ofNullable(source.getAddress()).ifPresent(entity::setAddress);
		ofNullable(source.getCareOf()).ifPresent(entity::setCareOf);
		ofNullable(source.getZipCode()).ifPresent(entity::setZipCode);
		ofNullable(source.getCity()).ifPresent(entity::setCity);
		ofNullable(source.getCountry()).ifPresent(entity::setCountry);
		ofNullable(source.getContactChannels()).ifPresent(channels -> entity.setContactChannels(new ArrayList<>(toTagEmbeddableList(channels))));
		return entity;
	}

	public static List<Stakeholder> toStakeholderList(final List<StakeholderEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(StakeholderMapper::toStakeholder)
			.toList();
	}

	public static List<StakeholderEntity> toStakeholderEntityList(final List<Stakeholder> stakeholders, final ErrandEntity errandEntity) {
		return ofNullable(stakeholders).orElse(emptyList()).stream()
			.map(stakeholder -> toStakeholderEntity(stakeholder, errandEntity))
			.toList();
	}
}
