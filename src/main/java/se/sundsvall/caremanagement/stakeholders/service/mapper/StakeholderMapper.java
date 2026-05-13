package se.sundsvall.caremanagement.stakeholders.service.mapper;

import java.util.ArrayList;
import java.util.List;
import se.sundsvall.caremanagement.stakeholders.api.model.Stakeholder;
import se.sundsvall.caremanagement.stakeholders.integration.db.model.StakeholderEntity;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.stakeholders.service.mapper.ContactChannelMapper.toContactChannelList;
import static se.sundsvall.caremanagement.stakeholders.service.mapper.ContactChannelMapper.toTagEmbeddableList;

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
				.withContactChannels(toContactChannelList(e.getContactChannels())))
			.orElse(null);
	}

	public static StakeholderEntity toStakeholderEntity(final Stakeholder stakeholder, final String errandId) {
		if (stakeholder == null) {
			return null;
		}
		return StakeholderEntity.create()
			.withErrandId(errandId)
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

	public static List<StakeholderEntity> toStakeholderEntityList(final List<Stakeholder> stakeholders, final String errandId) {
		return ofNullable(stakeholders).orElse(emptyList()).stream()
			.map(stakeholder -> toStakeholderEntity(stakeholder, errandId))
			.toList();
	}
}
