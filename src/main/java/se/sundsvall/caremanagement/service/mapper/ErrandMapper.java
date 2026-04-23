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
			.map(errandEntity -> Errand.create()
				.withId(errandEntity.getId())
				.withMunicipalityId(errandEntity.getMunicipalityId())
				.withNamespace(errandEntity.getNamespace())
				.withTitle(errandEntity.getTitle())
				.withCategory(errandEntity.getCategory())
				.withType(errandEntity.getType())
				.withStatus(errandEntity.getStatus())
				.withDescription(errandEntity.getDescription())
				.withPriority(errandEntity.getPriority())
				.withReporterUserId(errandEntity.getReporterUserId())
				.withAssignedUserId(errandEntity.getAssignedUserId())
				.withContactReason(ofNullable(errandEntity.getContactReason()).map(LookupEntity::getName).orElse(null))
				.withContactReasonDescription(errandEntity.getContactReasonDescription())
				.withExternalTags(toExternalTagList(errandEntity.getExternalTags()))
				.withStakeholders(toStakeholderList(errandEntity.getStakeholders()))
				.withParameters(toParameterList(errandEntity.getParameters()))
				.withProcessDefinitionName(errandEntity.getProcessDefinitionName())
				.withProcessInstanceId(errandEntity.getProcessInstanceId())
				.withCreated(errandEntity.getCreated())
				.withModified(errandEntity.getModified())
				.withTouched(errandEntity.getTouched()))
			.orElse(null);
	}

	public static ErrandEntity toErrandEntity(final Errand errand, final String namespace, final String municipalityId, final LookupEntity contactReason) {
		if (errand == null) {
			return null;
		}
		final var errandEntity = ErrandEntity.create()
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
			.withProcessDefinitionName(errand.getProcessDefinitionName())
			.withExternalTags(new ArrayList<>(toTagEmbeddableList(errand.getExternalTags())));
		return errandEntity
			.withStakeholders(new ArrayList<>(toStakeholderEntityList(errand.getStakeholders(), errandEntity)))
			.withParameters(new ArrayList<>(toParameterEntityList(errand.getParameters(), errandEntity)))
			.withAttachments(new ArrayList<>());
	}

	public static List<Errand> toErrandList(final List<ErrandEntity> entities) {
		return ofNullable(entities).orElse(emptyList()).stream()
			.map(ErrandMapper::toErrand)
			.toList();
	}

	public static FindErrandsResponse toFindErrandsResponse(final Page<ErrandEntity> page) {
		return ofNullable(page)
			.map(errandPage -> FindErrandsResponse.create()
				.withErrands(errandPage.getContent().stream().map(ErrandMapper::toErrand).toList())
				.withMetaData(new PagingAndSortingMetaData().withPageData(errandPage)))
			.orElseGet(() -> FindErrandsResponse.create()
				.withErrands(emptyList())
				.withMetaData(new PagingAndSortingMetaData()));
	}
}
