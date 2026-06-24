package se.sundsvall.caremanagement.formsnapshot.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * The immutable form snapshot of a single errand — the citizen-facing application form as it was rendered and answered,
 * stored verbatim in {@code payload} (the self-describing JSON envelope) with a SHA-256 {@code contentHash} over the
 * exact received bytes. Write-once: created with the errand and never updated; the unique constraint on
 * {@code errandId} enforces one snapshot per errand. {@code payload} is sensitive personal data and is deliberately
 * left out of {@code toString}.
 */
@Entity
@Table(name = "errand_form_snapshot",
	uniqueConstraints = @UniqueConstraint(name = "uq_form_snapshot_errand", columnNames = "errand_id"),
	indexes = @Index(name = "idx_form_snapshot_errand_id", columnList = "errand_id"))
public class FormSnapshotEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 255)
	private String errandId;

	@Column(name = "municipality_id", nullable = false, length = 32)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 128)
	private String namespace;

	@Column(name = "type_slug", nullable = false, length = 64)
	private String typeSlug;

	@Column(name = "schema_version", nullable = false, length = 32)
	private String schemaVersion;

	@Column(name = "form_definition_version", length = 64)
	private String formDefinitionVersion;

	@Column(name = "locale", length = 16)
	private String locale;

	@Column(name = "content_hash", nullable = false, length = 64)
	private String contentHash;

	@Column(name = "payload", nullable = false, length = LONG32)
	private String payload;

	@Column(name = "captured_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime capturedAt;

	@Column(name = "created", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	public static FormSnapshotEntity create() {
		return new FormSnapshotEntity();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getTypeSlug() {
		return typeSlug;
	}

	public String getSchemaVersion() {
		return schemaVersion;
	}

	public String getFormDefinitionVersion() {
		return formDefinitionVersion;
	}

	public String getLocale() {
		return locale;
	}

	public String getContentHash() {
		return contentHash;
	}

	public String getPayload() {
		return payload;
	}

	public OffsetDateTime getCapturedAt() {
		return capturedAt;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public FormSnapshotEntity withId(final String v) {
		this.id = v;
		return this;
	}

	public FormSnapshotEntity withErrandId(final String v) {
		this.errandId = v;
		return this;
	}

	public FormSnapshotEntity withMunicipalityId(final String v) {
		this.municipalityId = v;
		return this;
	}

	public FormSnapshotEntity withNamespace(final String v) {
		this.namespace = v;
		return this;
	}

	public FormSnapshotEntity withTypeSlug(final String v) {
		this.typeSlug = v;
		return this;
	}

	public FormSnapshotEntity withSchemaVersion(final String v) {
		this.schemaVersion = v;
		return this;
	}

	public FormSnapshotEntity withFormDefinitionVersion(final String v) {
		this.formDefinitionVersion = v;
		return this;
	}

	public FormSnapshotEntity withLocale(final String v) {
		this.locale = v;
		return this;
	}

	public FormSnapshotEntity withContentHash(final String v) {
		this.contentHash = v;
		return this;
	}

	public FormSnapshotEntity withPayload(final String v) {
		this.payload = v;
		return this;
	}

	public FormSnapshotEntity withCapturedAt(final OffsetDateTime v) {
		this.capturedAt = v;
		return this;
	}

	public FormSnapshotEntity withCreated(final OffsetDateTime v) {
		this.created = v;
		return this;
	}

	/**
	 * Intentionally omits {@code payload} — it is sensitive personal data and must never reach logs.
	 */
	@Override
	public String toString() {
		return "FormSnapshotEntity{id='" + id + "', errandId='" + errandId + "', municipalityId='" + municipalityId
			+ "', namespace='" + namespace + "', typeSlug='" + typeSlug + "', schemaVersion='" + schemaVersion
			+ "', formDefinitionVersion='" + formDefinitionVersion + "', locale='" + locale + "', contentHash='"
			+ contentHash + "', capturedAt=" + capturedAt + ", created=" + created + "}";
	}
}
