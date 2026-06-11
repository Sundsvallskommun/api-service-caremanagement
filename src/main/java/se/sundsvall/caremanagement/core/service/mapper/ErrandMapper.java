package se.sundsvall.caremanagement.core.service.mapper;

import java.util.List;
import org.springframework.data.domain.Page;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.api.model.FindErrandsResponse;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.dept44.models.api.paging.PagingAndSortingMetaData;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public final class ErrandMapper {

	private ErrandMapper() {}

	public static Errand toErrand(final ErrandEntity entity) {
		return ofNullable(entity)
			.map(e -> Errand.create()
				.withId(e.getId())
				.withMunicipalityId(e.getMunicipalityId())
				.withNamespace(e.getNamespace())
				.withErrandNumber(e.getErrandNumber())
				.withTypeSlug(e.getTypeSlug())
				.withTitle(e.getTitle())
				.withStatus(e.getStatus())
				.withDescription(e.getDescription())
				.withPriority(e.getPriority())
				.withReporterUserId(e.getReporterUserId())
				.withAssignedUserId(e.getAssignedUserId())
				.withProcessDefinitionName(e.getProcessDefinitionName())
				.withProcessInstanceId(e.getProcessInstanceId())
				.withCreated(e.getCreated())
				.withModified(e.getModified())
				.withTouched(e.getTouched()))
			.orElse(null);
	}

	public static ErrandEntity toErrandEntity(final Errand errand, final String namespace, final String municipalityId) {
		return ofNullable(errand)
			.map(source -> ErrandEntity.create()
				.withMunicipalityId(municipalityId)
				.withNamespace(namespace)
				.withErrandNumber(source.getErrandNumber())
				.withTypeSlug(source.getTypeSlug())
				.withTitle(source.getTitle())
				.withStatus(source.getStatus())
				.withDescription(source.getDescription())
				.withPriority(source.getPriority())
				.withReporterUserId(source.getReporterUserId())
				.withAssignedUserId(source.getAssignedUserId())
				.withProcessDefinitionName(source.getProcessDefinitionName()))
			.orElse(null);
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
