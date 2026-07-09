package se.sundsvall.caremanagement.formsnapshot.service.mapper;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import se.sundsvall.caremanagement.formsnapshot.api.model.FormSnapshot;

import static org.assertj.core.api.Assertions.assertThat;

class FormSnapshotMapperTest {

	@Test
	void sha256HexIsDeterministicAndKnown() {
		// SHA-256 of the ASCII string "abc" — a fixed, well-known vector.
		assertThat(FormSnapshotMapper.sha256Hex("abc"))
			.isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
			.isEqualTo(FormSnapshotMapper.sha256Hex("abc"));
	}

	@Test
	void sha256HexDiffersOnWhitespace() {
		// Reproducibility depends on exact bytes — re-serialization that reorders/space-normalizes would change the hash.
		assertThat(FormSnapshotMapper.sha256Hex("{\"a\":1}")).isNotEqualTo(FormSnapshotMapper.sha256Hex("{ \"a\": 1 }"));
	}

	@Test
	void toEntityLiftsMetadataAndStoresPayloadVerbatim() {
		final var capturedAt = OffsetDateTime.parse("2026-06-24T10:15:30+02:00");
		final var created = OffsetDateTime.parse("2026-06-24T10:15:31Z");
		final var payload = "{\"schemaVersion\":\"form-snapshot/1\",\"sections\":[]}";
		final var snapshot = FormSnapshot.create()
			.withSchemaVersion("form-snapshot/1")
			.withFormDefinitionVersion("eb-2026.06-r3")
			.withLocale("sv-SE")
			.withCapturedAt(capturedAt);

		final var entity = FormSnapshotMapper.toEntity("2281", "EB", "errand-1", "financial-assistance-new", payload, snapshot, created);

		assertThat(entity).hasNoNullFieldsOrPropertiesExcept("id");
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getNamespace()).isEqualTo("EB");
		assertThat(entity.getErrandId()).isEqualTo("errand-1");
		assertThat(entity.getTypeSlug()).isEqualTo("financial-assistance-new");
		assertThat(entity.getSchemaVersion()).isEqualTo("form-snapshot/1");
		assertThat(entity.getFormDefinitionVersion()).isEqualTo("eb-2026.06-r3");
		assertThat(entity.getLocale()).isEqualTo("sv-SE");
		assertThat(entity.getCapturedAt()).isEqualTo(capturedAt);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getPayload()).isEqualTo(payload);
		assertThat(entity.getContentHash()).isEqualTo(FormSnapshotMapper.sha256Hex(payload));
	}
}
