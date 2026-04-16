package se.sundsvall.caremanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import se.sundsvall.caremanagement.api.model.Errand;
import se.sundsvall.caremanagement.api.model.FindErrandsResponse;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.LookupEntity;
import se.sundsvall.dept44.models.api.paging.PagingAndSortingMetaData;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.caremanagement.service.mapper.ExternalTagMapper.toExternalTagList;
import static se.sundsvall.caremanagement.service.mapper.ExternalTagMapper.toTagEmbeddableList;
import static se.sundsvall.caremanagement.service.mapper.ParameterMapper.toParameterEntityList;
import static se.sundsvall.caremanagement.service.mapper.ParameterMapper.toParameterList;
import static se.sundsvall.caremanagement.service.mapper.StakeholderMapper.toStakeholderEntityList;
import static se.sundsvall.caremanagement.service.mapper.StakeholderMapper.toStakeholderList;

public final class ErrandMapper {

	private ErrandMapper() {}

	public static Errand toErrand(final ErrandEntity entity) {
		return ofNullable(entity)
			.map(e -> Errand.create()
				.withId(e.getId())
				.withMunicipalityId(e.getMunicipalityId())
				.withNamespace(e.getNamespace())
				.withTitle(e.getTitle())
				.withCategory(e.getCategory())
				.withType(e.getType())
				.withStatus(e.getStatus())
				.withDescription(e.getDescription())
				.withPriority(e.getPriority())
				.withReporterUserId(e.getReporterUserId())
				.withAssignedUserId(e.getAssignedUserId())
				.withContactReason(ofNullable(e.getContactReason()).map(LookupEntity::getName).orElse(null))
				.withContactReasonDescription(e.getContactReasonDescription())
				.withExternalTags(toExternalTagList(e.getExternalTags()))
				.withStakeholders(toStakeholderList(e.getStakeholders()))
				.withParameters(toParameterList(e.getParameters()))
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withTouched(e.getTouched()))
			.orElse(null);
	}

	public static ErrandEntity toErrandEntity(final Errand errand, final String namespace, final String municipalityId, final LookupEntity contactReason) {
		if (errand == null) {
			return null;
		}
		final var entity = ErrandEntity.create()
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withTitle(errand.getTitle())
			.withCategory(errand.getCategory())
			.withType(errand.getType())
			.withStatus(errand.getStatus())
			.withDescription(errand.getDescription())
			.withPriority(errand.getPriority())
			.withReporterUserId(errand.getReporterUserId())
			.withAssignedUserId(errand.getAssignedUserId())
			.withContactReason(contactReason)
			.withContactReasonDescription(errand.getContactReasonDescription())
			.withExternalTags(new ArrayList<>(toTagEmbeddableList(errand.getExternalTags())));
		entity.setStakeholders(new ArrayList<>(toStakeholderEntityList(errand.getStakeholders(), entity)));
		entity.setParameters(new ArrayList<>(toParameterEntityList(errand.getParameters(), entity)));
		entity.setAttachments(new ArrayList<>());
		return entity;
	}

	public static List<Errand> toErrandList(final List<ErrandEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ErrandMapper::toErrand)
			.toList();
	}

	public static FindErrandsResponse toFindErrandsResponse(final Page<ErrandEntity> page) {
		if (page == null) {
			return FindErrandsResponse.create()
				.withErrands(emptyList())
				.withMetaData(new PagingAndSortingMetaData());
		}
		return FindErrandsResponse.create()
			.withErrands(page.getContent().stream().map(ErrandMapper::toErrand).toList())
			.withMetaData(new PagingAndSortingMetaData().withPageData(page));
	}
}
