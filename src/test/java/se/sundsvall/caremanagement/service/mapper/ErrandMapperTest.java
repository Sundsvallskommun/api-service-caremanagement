package se.sundsvall.caremanagement.service.mapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import se.sundsvall.caremanagement.api.model.Errand;
import se.sundsvall.caremanagement.api.model.ExternalTag;
import se.sundsvall.caremanagement.api.model.Parameter;
import se.sundsvall.caremanagement.api.model.Stakeholder;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.LookupEntity;
import se.sundsvall.caremanagement.integration.db.model.LookupKind;
import se.sundsvall.caremanagement.integration.db.model.ParameterEntity;
import se.sundsvall.caremanagement.integration.db.model.StakeholderEntity;
import se.sundsvall.caremanagement.integration.db.model.TagEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;

class ErrandMapperTest {

	@Test
	void toErrand_maps() {
		final var created = OffsetDateTime.now().minusDays(1);
		final var modified = OffsetDateTime.now();
		final var entity = ErrandEntity.create()
			.withId("eid")
			.withMunicipalityId("2281")
			.withNamespace("ns")
			.withTitle("title")
			.withCategory("cat")
			.withType("type")
			.withStatus("NEW")
			.withDescription("desc")
			.withPriority("HIGH")
			.withReporterUserId("rep")
			.withAssignedUserId("ass")
			.withContactReason(LookupEntity.create().withKind(LookupKind.CONTACT_REASON).withName("PHONE"))
			.withContactReasonDescription("crd")
			.withExternalTags(new ArrayList<>(List.of(TagEmbeddable.create().withKey("k").withValue("v"))))
			.withStakeholders(new ArrayList<>(List.of(StakeholderEntity.create().withId("sid").withRole("APPLICANT"))))
			.withParameters(new ArrayList<>(List.of(ParameterEntity.create().withId("pid").withKey("k"))))
			.withProcessDefinitionName("Handläggning")
			.withProcessInstanceId("pi-1")
			.withCreated(created)
			.withModified(modified);

		final var result = ErrandMapper.toErrand(entity);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo("eid");
		assertThat(result.getMunicipalityId()).isEqualTo("2281");
		assertThat(result.getNamespace()).isEqualTo("ns");
		assertThat(result.getTitle()).isEqualTo("title");
		assertThat(result.getCategory()).isEqualTo("cat");
		assertThat(result.getType()).isEqualTo("type");
		assertThat(result.getStatus()).isEqualTo("NEW");
		assertThat(result.getDescription()).isEqualTo("desc");
		assertThat(result.getPriority()).isEqualTo("HIGH");
		assertThat(result.getReporterUserId()).isEqualTo("rep");
		assertThat(result.getAssignedUserId()).isEqualTo("ass");
		assertThat(result.getContactReason()).isEqualTo("PHONE");
		assertThat(result.getContactReasonDescription()).isEqualTo("crd");
		assertThat(result.getExternalTags()).hasSize(1);
		assertThat(result.getStakeholders()).hasSize(1);
		assertThat(result.getParameters()).hasSize(1);
		assertThat(result.getProcessDefinitionName()).isEqualTo("Handläggning");
		assertThat(result.getProcessInstanceId()).isEqualTo("pi-1");
		assertThat(result.getCreated()).isEqualTo(created);
		assertThat(result.getModified()).isEqualTo(modified);
	}

	@Test
	void toErrand_nullContactReason() {
		final var entity = ErrandEntity.create().withId("eid").withContactReason(null);

		final var result = ErrandMapper.toErrand(entity);

		assertThat(result).isNotNull();
		assertThat(result.getContactReason()).isNull();
	}

	@Test
	void toErrand_nullReturnsNull() {
		assertThat(ErrandMapper.toErrand(null)).isNull();
	}

	@Test
	void toErrandEntity_maps() {
		final var contactReason = LookupEntity.create().withKind(LookupKind.CONTACT_REASON).withName("PHONE");
		final var errand = Errand.create()
			.withTitle("title")
			.withCategory("cat")
			.withType("type")
			.withStatus("NEW")
			.withDescription("desc")
			.withPriority("HIGH")
			.withReporterUserId("rep")
			.withAssignedUserId("ass")
			.withContactReason("PHONE")
			.withContactReasonDescription("crd")
			.withExternalTags(List.of(ExternalTag.create().withKey("k").withValue("v")))
			.withStakeholders(List.of(Stakeholder.create().withRole("APPLICANT")))
			.withParameters(List.of(Parameter.create().withKey("k")))
			.withProcessDefinitionName("Handläggning");

		final var result = ErrandMapper.toErrandEntity(errand, "ns", "2281", contactReason);

		assertThat(result).isNotNull();
		assertThat(result.getMunicipalityId()).isEqualTo("2281");
		assertThat(result.getNamespace()).isEqualTo("ns");
		assertThat(result.getTitle()).isEqualTo("title");
		assertThat(result.getContactReason()).isSameAs(contactReason);
		assertThat(result.getExternalTags()).hasSize(1);
		assertThat(result.getStakeholders()).hasSize(1);
		assertThat(result.getStakeholders().getFirst().getErrandEntity()).isSameAs(result);
		assertThat(result.getParameters()).hasSize(1);
		assertThat(result.getParameters().getFirst().getErrandEntity()).isSameAs(result);
		assertThat(result.getProcessDefinitionName()).isEqualTo("Handläggning");
		assertThat(result.getProcessInstanceId()).isNull();
		assertThat(result.getAttachments()).isNotNull().isEmpty();
	}

	@Test
	void toErrandEntity_nullReturnsNull() {
		assertThat(ErrandMapper.toErrandEntity(null, "ns", "2281", null)).isNull();
	}

	@Test
	void toErrandList_maps() {
		final var result = ErrandMapper.toErrandList(List.of(
			ErrandEntity.create().withId("a"),
			ErrandEntity.create().withId("b")));

		assertThat(result).hasSize(2);
	}

	@Test
	void toErrandList_nullReturnsEmpty() {
		assertThat(ErrandMapper.toErrandList(null)).isEmpty();
	}

	@Test
	void toFindErrandsResponse_maps() {
		final var page = new PageImpl<>(List.of(
			ErrandEntity.create().withId("a"),
			ErrandEntity.create().withId("b")), Pageable.ofSize(20), 2);

		final var result = ErrandMapper.toFindErrandsResponse(page);

		assertThat(result).isNotNull();
		assertThat(result.getErrands()).hasSize(2);
		assertThat(result.getMetaData()).isNotNull();
		assertThat(result.getMetaData().getCount()).isEqualTo(2);
		assertThat(result.getMetaData().getTotalRecords()).isEqualTo(2);
	}

	@Test
	void toFindErrandsResponse_nullReturnsEmpty() {
		final var result = ErrandMapper.toFindErrandsResponse(null);

		assertThat(result).isNotNull();
		assertThat(result.getErrands()).isEmpty();
		assertThat(result.getMetaData()).isNotNull();
	}
}
