package se.sundsvall.caremanagement.core.service.mapper;

import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.core.api.model.PatchErrand;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;

import static org.assertj.core.api.Assertions.assertThat;

class PatchMapperTest {

	private static ErrandEntity existingEntity() {
		return ErrandEntity.create()
			.withId("cb20c51f-fcf3-42c0-b613-de563634a8ec")
			.withMunicipalityId("2281")
			.withNamespace("FINANCIAL_ASSISTANCE")
			.withErrandNumber("EB-26060071")
			.withTypeSlug("ateransokan-hyra")
			.withTitle("Old title")
			.withStatus("NEW")
			.withDescription("Old description")
			.withPriority("LOW")
			.withReporterUserId("oldReporter")
			.withAssignedUserId("oldAssignee")
			.withApplicantName("Anna Andersson")
			.withProcessDefinitionName("Handlaggning av arende")
			.withProcessInstanceId("a-process-instance-id");
	}

	@Test
	void patchErrandUpdatesEverySettableFieldWhenPresent() {
		final var entity = existingEntity();
		final var patch = PatchErrand.create()
			.withTitle("New title")
			.withStatus("IN_PROGRESS")
			.withDescription("New description")
			.withPriority("HIGH")
			.withReporterUserId("newReporter")
			.withAssignedUserId("newAssignee");

		final var result = PatchMapper.patchErrand(entity, patch);

		// patches mutate and return the same instance
		assertThat(result).isSameAs(entity);
		// every patchable field is overwritten
		assertThat(result.getTitle()).isEqualTo("New title");
		assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
		assertThat(result.getDescription()).isEqualTo("New description");
		assertThat(result.getPriority()).isEqualTo("HIGH");
		assertThat(result.getReporterUserId()).isEqualTo("newReporter");
		assertThat(result.getAssignedUserId()).isEqualTo("newAssignee");
		// non-patchable envelope fields are left untouched
		assertThat(result.getId()).isEqualTo("cb20c51f-fcf3-42c0-b613-de563634a8ec");
		assertThat(result.getMunicipalityId()).isEqualTo("2281");
		assertThat(result.getNamespace()).isEqualTo("FINANCIAL_ASSISTANCE");
		assertThat(result.getErrandNumber()).isEqualTo("EB-26060071");
		assertThat(result.getTypeSlug()).isEqualTo("ateransokan-hyra");
		assertThat(result.getApplicantName()).isEqualTo("Anna Andersson");
		assertThat(result.getProcessDefinitionName()).isEqualTo("Handlaggning av arende");
		assertThat(result.getProcessInstanceId()).isEqualTo("a-process-instance-id");
	}

	@Test
	void patchErrandNullFieldsLeaveTargetUnchanged() {
		final var entity = existingEntity();
		// empty patch — all fields null — must change nothing
		final var result = PatchMapper.patchErrand(entity, PatchErrand.create());

		assertThat(result).isSameAs(entity);
		assertThat(result.getTitle()).isEqualTo("Old title");
		assertThat(result.getStatus()).isEqualTo("NEW");
		assertThat(result.getDescription()).isEqualTo("Old description");
		assertThat(result.getPriority()).isEqualTo("LOW");
		assertThat(result.getReporterUserId()).isEqualTo("oldReporter");
		assertThat(result.getAssignedUserId()).isEqualTo("oldAssignee");
	}

	@Test
	void patchErrandPatchesOnlyTheNonNullFieldsAndKeepsTheRest() {
		final var entity = existingEntity();
		// only status + assignedUserId set; the other four stay as-is
		final var patch = PatchErrand.create()
			.withStatus("CLOSED")
			.withAssignedUserId("newAssignee");

		final var result = PatchMapper.patchErrand(entity, patch);

		// updated
		assertThat(result.getStatus()).isEqualTo("CLOSED");
		assertThat(result.getAssignedUserId()).isEqualTo("newAssignee");
		// untouched (patch field was null)
		assertThat(result.getTitle()).isEqualTo("Old title");
		assertThat(result.getDescription()).isEqualTo("Old description");
		assertThat(result.getPriority()).isEqualTo("LOW");
		assertThat(result.getReporterUserId()).isEqualTo("oldReporter");
	}

	@Test
	void patchErrandNullEntityReturnsNull() {
		assertThat(PatchMapper.patchErrand(null, PatchErrand.create().withTitle("New title"))).isNull();
	}

	@Test
	void patchErrandNullPatchReturnsEntityUnchanged() {
		final var entity = existingEntity();

		final var result = PatchMapper.patchErrand(entity, null);

		assertThat(result).isSameAs(entity);
		assertThat(result.getTitle()).isEqualTo("Old title");
		assertThat(result.getStatus()).isEqualTo("NEW");
		assertThat(result.getDescription()).isEqualTo("Old description");
		assertThat(result.getPriority()).isEqualTo("LOW");
		assertThat(result.getReporterUserId()).isEqualTo("oldReporter");
		assertThat(result.getAssignedUserId()).isEqualTo("oldAssignee");
	}
}
