package se.sundsvall.caremanagement.formsnapshot.service.mapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import se.sundsvall.caremanagement.formsnapshot.api.model.FormSnapshot;
import se.sundsvall.caremanagement.formsnapshot.integration.db.model.FormSnapshotEntity;

/**
 * Maps between the {@link FormSnapshot} envelope and its {@link FormSnapshotEntity}, and computes the integrity hash.
 *
 * <p>
 * The {@code contentHash} is taken over the <em>exact received payload bytes</em> (UTF-8), never over a re-serialized
 * object — re-serialization would reorder keys and break reproducibility. The persisted {@code payload} is likewise the
 * verbatim received JSON; the parsed {@link FormSnapshot} is used only to lift the envelope metadata onto indexed
 * columns.
 * </p>
 */
public final class FormSnapshotMapper {

	private FormSnapshotMapper() {}

	/** SHA-256 (hex) of the payload's UTF-8 bytes — the immutability/integrity proof for the stored snapshot. */
	public static String sha256Hex(final String payload) {
		try {
			final var digest = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (final NoSuchAlgorithmException e) {
			// SHA-256 is mandated by the JLS on every JVM — unreachable in practice.
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	/**
	 * Build the write-once entity from the verbatim payload and the parsed envelope. The payload is stored as received;
	 * the envelope supplies the indexed metadata columns.
	 */
	public static FormSnapshotEntity toEntity(final String municipalityId, final String namespace, final String errandId,
		final String typeSlug, final String payload, final FormSnapshot snapshot, final OffsetDateTime created) {
		return FormSnapshotEntity.create()
			.withErrandId(errandId)
			.withMunicipalityId(municipalityId)
			.withNamespace(namespace)
			.withTypeSlug(typeSlug)
			.withSchemaVersion(snapshot.getSchemaVersion())
			.withFormDefinitionVersion(snapshot.getFormDefinitionVersion())
			.withLocale(snapshot.getLocale())
			.withContentHash(sha256Hex(payload))
			.withPayload(payload)
			.withCapturedAt(snapshot.getCapturedAt())
			.withCreated(created);
	}
}
