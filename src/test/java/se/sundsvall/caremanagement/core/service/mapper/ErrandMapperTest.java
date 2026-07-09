package se.sundsvall.caremanagement.core.service.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ErrandMapperTest {

	private static final OffsetDateTime CREATED = OffsetDateTime.parse("2026-06-20T08:15:00Z");
	private static final OffsetDateTime MODIFIED = OffsetDateTime.parse("2026-06-22T09:30:00Z");
	private static final OffsetDateTime TOUCHED = OffsetDateTime.parse("2026-06-23T10:45:00Z");

	private static ErrandEntity fullEntity() {
		return ErrandEntity.create()
			.withId("cb20c51f-fcf3-42c0-b613-de563634a8ec")
			.withMunicipalityId("2281")
			.withNamespace("FINANCIAL_ASSISTANCE")
			.withErrandNumber("EB-26060071")
			.withTypeSlug("ateransokan-hyra")
			.withTitle("Ateransokan om hyra")
			.withStatus("NEW")
			.withDescription("Manadsansokan for juni")
			.withPriority("HIGH")
			.withReporterUserId("joe01doe")
			.withAssignedUserId("jane02doe")
			.withApplicantName("Anna Andersson")
			.withProcessDefinitionName("Handlaggning av arende")
			.withProcessInstanceId("a-process-instance-id")
			.withCreated(CREATED)
			.withModified(MODIFIED)
			.withTouched(TOUCHED);
	}

	@Test
	void toErrandMapsEveryField() {
		final var result = ErrandMapper.toErrand(fullEntity());

		assertThat(result).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(result.getId()).isEqualTo("cb20c51f-fcf3-42c0-b613-de563634a8ec");
		assertThat(result.getMunicipalityId()).isEqualTo("2281");
		assertThat(result.getNamespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		assertThat(result.getErrandNumber()).isEqualTo("EB-26060071");
		assertThat(result.getTypeSlug()).isEqualTo("ateransokan-hyra");
		assertThat(result.getTitle()).isEqualTo("Ateransokan om hyra");
		assertThat(result.getStatus()).isEqualTo("NEW");
		assertThat(result.getDescription()).isEqualTo("Manadsansokan for juni");
		assertThat(result.getPriority()).isEqualTo("HIGH");
		assertThat(result.getReporterUserId()).isEqualTo("joe01doe");
		assertThat(result.getAssignedUserId()).isEqualTo("jane02doe");
		assertThat(result.getApplicantName()).isEqualTo("Anna Andersson");
		assertThat(result.getProcessDefinitionName()).isEqualTo("Handlaggning av arende");
		assertThat(result.getProcessInstanceId()).isEqualTo("a-process-instance-id");
		assertThat(result.getCreated()).isEqualTo(CREATED);
		assertThat(result.getModified()).isEqualTo(MODIFIED);
		assertThat(result.getTouched()).isEqualTo(TOUCHED);
	}

	@Test
	void toErrandNullReturnsNull() {
		assertThat(ErrandMapper.toErrand(null)).isNull();
	}

	@Test
	void toErrandEntityMapsSourceFieldsAndUsesNamespaceAndMunicipalityArgs() {
		final var source = Errand.create()
			.withId("ignored-id")
			.withMunicipalityId("ignored-municipality")
			.withNamespace("ignored-namespace")
			.withErrandNumber("EB-26060071")
			.withTypeSlug("ateransokan-hyra")
			.withTitle("Ateransokan om hyra")
			.withStatus("NEW")
			.withDescription("Manadsansokan for juni")
			.withPriority("HIGH")
			.withReporterUserId("joe01doe")
			.withAssignedUserId("jane02doe")
			.withApplicantName("Anna Andersson")
			.withProcessDefinitionName("Handlaggning av arende")
			.withProcessInstanceId("ignored-process-instance")
			.withCreated(CREATED)
			.withModified(MODIFIED)
			.withTouched(TOUCHED);

		final var result = ErrandMapper.toErrandEntity(source, "FINANCIAL_ASSISTANCE", "2281");

		assertThat(result).isNotNull()
			.hasNoNullFieldsOrPropertiesExcept("id", "applicantName", "processInstanceId", "created", "modified", "touched");
		// namespace + municipalityId come from the method args, NOT from the source DTO.
		assertThat(result.getMunicipalityId()).isEqualTo("2281");
		assertThat(result.getNamespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		// fields copied from the source DTO.
		assertThat(result.getErrandNumber()).isEqualTo("EB-26060071");
		assertThat(result.getTypeSlug()).isEqualTo("ateransokan-hyra");
		assertThat(result.getTitle()).isEqualTo("Ateransokan om hyra");
		assertThat(result.getStatus()).isEqualTo("NEW");
		assertThat(result.getDescription()).isEqualTo("Manadsansokan for juni");
		assertThat(result.getPriority()).isEqualTo("HIGH");
		assertThat(result.getReporterUserId()).isEqualTo("joe01doe");
		assertThat(result.getAssignedUserId()).isEqualTo("jane02doe");
		assertThat(result.getProcessDefinitionName()).isEqualTo("Handlaggning av arende");
		// read-only / server-owned fields are intentionally NOT carried over from the DTO.
		assertThat(result.getId()).isNull();
		assertThat(result.getApplicantName()).isNull();
		assertThat(result.getProcessInstanceId()).isNull();
		assertThat(result.getCreated()).isNull();
		assertThat(result.getModified()).isNull();
		// touched falls back to created/modified when unset; both null here so it stays null.
		assertThat(result.getTouched()).isNull();
	}

	@Test
	void toErrandEntityNullReturnsNull() {
		assertThat(ErrandMapper.toErrandEntity(null, "FINANCIAL_ASSISTANCE", "2281")).isNull();
	}

	@Test
	void toErrandListMapsEveryItem() {
		final var first = ErrandEntity.create().withId("id-1").withTitle("First");
		final var second = ErrandEntity.create().withId("id-2").withTitle("Second");

		final var result = ErrandMapper.toErrandList(List.of(first, second));

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getId()).isEqualTo("id-1");
		assertThat(result.get(0).getTitle()).isEqualTo("First");
		assertThat(result.get(1).getId()).isEqualTo("id-2");
		assertThat(result.get(1).getTitle()).isEqualTo("Second");
	}

	@Test
	void toErrandListNullReturnsEmpty() {
		assertThat(ErrandMapper.toErrandList(null)).isEmpty();
	}

	@Test
	void toFindErrandsResponseMapsContentAndMetaData() {
		final var entity = ErrandEntity.create().withId("id-1").withTitle("First");
		final var pageRequest = PageRequest.of(1, 5, Sort.by("title"));
		final var page = new PageImpl<>(List.of(entity), pageRequest, 11);

		final var result = ErrandMapper.toFindErrandsResponse(page);

		assertThat(result).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(result.getErrands()).hasSize(1);
		assertThat(result.getErrands().getFirst().getId()).isEqualTo("id-1");
		assertThat(result.getErrands().getFirst().getTitle()).isEqualTo("First");
		assertThat(result.getMetaData()).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(result.getMetaData().getPage()).isEqualTo(2);
		assertThat(result.getMetaData().getLimit()).isEqualTo(5);
		assertThat(result.getMetaData().getCount()).isEqualTo(1);
		assertThat(result.getMetaData().getTotalRecords()).isEqualTo(11);
		assertThat(result.getMetaData().getTotalPages()).isEqualTo(3);
	}

	@Test
	void toFindErrandsResponseNullReturnsEmptyEnvelope() {
		final var result = ErrandMapper.toFindErrandsResponse(null);

		assertThat(result).isNotNull();
		assertThat(result.getErrands()).isEmpty();
		assertThat(result.getMetaData()).isNotNull();
	}
}
