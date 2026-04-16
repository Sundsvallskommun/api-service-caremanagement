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
			.map(e -> Stakeholder.create()
				.withId(e.getId())
				.withExternalId(e.getExternalId())
				.withExternalIdType(e.getExternalIdType())
				.withRole(e.getRole())
				.withFirstName(e.getFirstName())
				.withLastName(e.getLastName())
				.withOrganizationName(e.getOrganizationName())
				.withAddress(e.getAddress())
				.withCareOf(e.getCareOf())
				.withZipCode(e.getZipCode())
				.withCity(e.getCity())
				.withCountry(e.getCountry())
				.withContactChannels(toContactChannelList(e.getContactChannels()))
				.withParameters(toStakeholderParameterList(e.getParameters())))
			.orElse(null);
	}

	public static StakeholderEntity toStakeholderEntity(final Stakeholder stakeholder, final ErrandEntity errandEntity) {
		if (stakeholder == null) {
			return null;
		}
		final var entity = StakeholderEntity.create()
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
		entity.setParameters(new ArrayList<>(toStakeholderParameterEntityList(stakeholder.getParameters(), entity)));
		return entity;
	}

	public static StakeholderEntity updateStakeholderEntity(final StakeholderEntity entity, final Stakeholder source) {
		if (entity == null || source == null) {
			return entity;
		}
		entity.setExternalId(source.getExternalId());
		entity.setExternalIdType(source.getExternalIdType());
		entity.setRole(source.getRole());
		entity.setFirstName(source.getFirstName());
		entity.setLastName(source.getLastName());
		entity.setOrganizationName(source.getOrganizationName());
		entity.setAddress(source.getAddress());
		entity.setCareOf(source.getCareOf());
		entity.setZipCode(source.getZipCode());
		entity.setCity(source.getCity());
		entity.setCountry(source.getCountry());
		entity.setContactChannels(new ArrayList<>(toTagEmbeddableList(source.getContactChannels())));
		return entity;
	}

	public static List<Stakeholder> toStakeholderList(final List<StakeholderEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(StakeholderMapper::toStakeholder)
			.toList();
	}

	public static List<StakeholderEntity> toStakeholderEntityList(final List<Stakeholder> stakeholders, final ErrandEntity errandEntity) {
		return ofNullable(stakeholders).orElse(emptyList()).stream()
			.map(s -> toStakeholderEntity(s, errandEntity))
			.toList();
	}
}
