package se.sundsvall.caremanagement.service.mapper;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.api.model.ExternalTag;
import se.sundsvall.caremanagement.api.model.PatchErrand;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.integration.db.model.LookupEntity;
import se.sundsvall.caremanagement.integration.db.model.LookupKind;
import se.sundsvall.caremanagement.integration.db.model.TagEmbeddable;

import static org.assertj.core.api.Assertions.assertThat;

class PatchMapperTest {

	@Test
	void patchErrand_appliesAllNonNullFields() {
		final var existingContactReason = LookupEntity.create().withKind(LookupKind.CONTACT_REASON).withName("OLD");
		final var entity = ErrandEntity.create()
			.withTitle("old-title")
			.withCategory("old-cat")
			.withType("old-type")
			.withStatus("old-status")
			.withDescription("old-desc")
			.withPriority("LOW")
			.withReporterUserId("old-rep")
			.withAssignedUserId("old-ass")
			.withContactReason(existingContactReason)
			.withContactReasonDescription("old-crd")
			.withExternalTags(new ArrayList<>(List.of(TagEmbeddable.create().withKey("old").withValue("x"))));

		final var patch = PatchErrand.create()
			.withTitle("new-title")
			.withCategory("new-cat")
			.withType("new-type")
			.withStatus("new-status")
			.withDescription("new-desc")
			.withPriority("HIGH")
			.withReporterUserId("new-rep")
			.withAssignedUserId("new-ass")
			.withContactReason("NEW")
			.withContactReasonDescription("new-crd")
			.withExternalTags(List.of(ExternalTag.create().withKey("k").withValue("v")));

		final var resolved = LookupEntity.create().withKind(LookupKind.CONTACT_REASON).withName("NEW");
		final var result = PatchMapper.patchErrand(entity, patch, resolved);

		assertThat(result).isSameAs(entity);
		assertThat(entity.getTitle()).isEqualTo("new-title");
		assertThat(entity.getCategory()).isEqualTo("new-cat");
		assertThat(entity.getType()).isEqualTo("new-type");
		assertThat(entity.getStatus()).isEqualTo("new-status");
		assertThat(entity.getDescription()).isEqualTo("new-desc");
		assertThat(entity.getPriority()).isEqualTo("HIGH");
		assertThat(entity.getReporterUserId()).isEqualTo("new-rep");
		assertThat(entity.getAssignedUserId()).isEqualTo("new-ass");
		assertThat(entity.getContactReasonDescription()).isEqualTo("new-crd");
		assertThat(entity.getContactReason()).isSameAs(resolved);
		assertThat(entity.getExternalTags()).hasSize(1);
		assertThat(entity.getExternalTags().getFirst().getKey()).isEqualTo("k");
	}

	@Test
	void patchErrand_nullFieldsLeaveEntityUntouched() {
		final var existingContactReason = LookupEntity.create().withKind(LookupKind.CONTACT_REASON).withName("OLD");
		final var existingTags = new ArrayList<>(List.of(TagEmbeddable.create().withKey("old").withValue("x")));
		final var entity = ErrandEntity.create()
			.withTitle("kept")
			.withCategory("kept")
			.withType("kept")
			.withStatus("kept")
			.withDescription("kept")
			.withPriority("kept")
			.withReporterUserId("kept")
			.withAssignedUserId("kept")
			.withContactReason(existingContactReason)
			.withContactReasonDescription("kept")
			.withExternalTags(existingTags);

		PatchMapper.patchErrand(entity, PatchErrand.create(), null);

		assertThat(entity.getTitle()).isEqualTo("kept");
		assertThat(entity.getCategory()).isEqualTo("kept");
		assertThat(entity.getType()).isEqualTo("kept");
		assertThat(entity.getStatus()).isEqualTo("kept");
		assertThat(entity.getDescription()).isEqualTo("kept");
		assertThat(entity.getPriority()).isEqualTo("kept");
		assertThat(entity.getReporterUserId()).isEqualTo("kept");
		assertThat(entity.getAssignedUserId()).isEqualTo("kept");
		assertThat(entity.getContactReason()).isSameAs(existingContactReason);
		assertThat(entity.getContactReasonDescription()).isEqualTo("kept");
		assertThat(entity.getExternalTags()).isSameAs(existingTags);
	}

	@Test
	void patchErrand_contactReasonNullInPatchLeavesEntityContactReasonUntouched() {
		final var existing = LookupEntity.create().withKind(LookupKind.CONTACT_REASON).withName("OLD");
		final var entity = ErrandEntity.create().withContactReason(existing);

		PatchMapper.patchErrand(entity, PatchErrand.create().withTitle("t"), null);

		assertThat(entity.getContactReason()).isSameAs(existing);
	}

	@Test
	void patchErrand_nullEntityReturnsNull() {
		assertThat(PatchMapper.patchErrand(null, PatchErrand.create(), null)).isNull();
	}

	@Test
	void patchErrand_nullPatchReturnsEntity() {
		final var entity = ErrandEntity.create().withTitle("kept");
		final var result = PatchMapper.patchErrand(entity, null, null);
		assertThat(result).isSameAs(entity);
		assertThat(entity.getTitle()).isEqualTo("kept");
	}
}
