package se.sundsvall.caremanagement.formsnapshot.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * A self-describing, re-renderable snapshot of the citizen-facing application form exactly as it was rendered and
 * answered in Mina sidor. Every node carries its own display text, so a generic renderer can walk
 * {@code sections → fields → (options | items) → answer.display} and reproduce what the applicant saw without any
 * access to the frontend that produced it.
 *
 * <p>
 * {@code schemaVersion} is <em>our</em> envelope contract version (lets the storage shape evolve);
 * {@code formDefinitionVersion} is the frontend's form/i18n bundle version (what the verksamhet means by "the June
 * form").
 * </p>
 */
@Schema(description = "Self-describing snapshot of the form as it was rendered and answered.")
public class FormSnapshot {

	@NotBlank
	@Schema(description = "The snapshot envelope contract version (server-owned)", examples = "form-snapshot/1", requiredMode = Schema.RequiredMode.REQUIRED)
	private String schemaVersion;

	@Schema(description = "The frontend form / i18n bundle version that produced this snapshot", examples = "eb-2026.06-r3")
	private String formDefinitionVersion;

	@Schema(description = "The errand type slug the form belongs to", examples = "financial-assistance-new")
	private String typeSlug;

	@Schema(description = "The locale the form was rendered in", examples = "sv-SE")
	private String locale;

	@Schema(description = "When the form was rendered/submitted, per the client clock", examples = "2026-06-24T10:15:30+02:00")
	private OffsetDateTime capturedAt;

	@Schema(description = "The form title the applicant saw", examples = "Ansökan om ekonomiskt bistånd")
	private String title;

	@Valid
	@NotEmpty
	@Schema(description = "The sections of the form, in render order", requiredMode = Schema.RequiredMode.REQUIRED)
	private List<FormSnapshotSection> sections;

	@Valid
	@Schema(description = "The attestation the applicant accepted, if any")
	private FormSnapshotAttestation attestation;

	public static FormSnapshot create() {
		return new FormSnapshot();
	}

	public String getSchemaVersion() {
		return schemaVersion;
	}

	public void setSchemaVersion(final String schemaVersion) {
		this.schemaVersion = schemaVersion;
	}

	public FormSnapshot withSchemaVersion(final String schemaVersion) {
		this.schemaVersion = schemaVersion;
		return this;
	}

	public String getFormDefinitionVersion() {
		return formDefinitionVersion;
	}

	public void setFormDefinitionVersion(final String formDefinitionVersion) {
		this.formDefinitionVersion = formDefinitionVersion;
	}

	public FormSnapshot withFormDefinitionVersion(final String formDefinitionVersion) {
		this.formDefinitionVersion = formDefinitionVersion;
		return this;
	}

	public String getTypeSlug() {
		return typeSlug;
	}

	public void setTypeSlug(final String typeSlug) {
		this.typeSlug = typeSlug;
	}

	public FormSnapshot withTypeSlug(final String typeSlug) {
		this.typeSlug = typeSlug;
		return this;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(final String locale) {
		this.locale = locale;
	}

	public FormSnapshot withLocale(final String locale) {
		this.locale = locale;
		return this;
	}

	public OffsetDateTime getCapturedAt() {
		return capturedAt;
	}

	public void setCapturedAt(final OffsetDateTime capturedAt) {
		this.capturedAt = capturedAt;
	}

	public FormSnapshot withCapturedAt(final OffsetDateTime capturedAt) {
		this.capturedAt = capturedAt;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public FormSnapshot withTitle(final String title) {
		this.title = title;
		return this;
	}

	public List<FormSnapshotSection> getSections() {
		return sections;
	}

	public void setSections(final List<FormSnapshotSection> sections) {
		this.sections = sections;
	}

	public FormSnapshot withSections(final List<FormSnapshotSection> sections) {
		this.sections = sections;
		return this;
	}

	public FormSnapshotAttestation getAttestation() {
		return attestation;
	}

	public void setAttestation(final FormSnapshotAttestation attestation) {
		this.attestation = attestation;
	}

	public FormSnapshot withAttestation(final FormSnapshotAttestation attestation) {
		this.attestation = attestation;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FormSnapshot that = (FormSnapshot) o;
		return Objects.equals(schemaVersion, that.schemaVersion) && Objects.equals(formDefinitionVersion, that.formDefinitionVersion)
			&& Objects.equals(typeSlug, that.typeSlug) && Objects.equals(locale, that.locale) && Objects.equals(capturedAt, that.capturedAt)
			&& Objects.equals(title, that.title) && Objects.equals(sections, that.sections) && Objects.equals(attestation, that.attestation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(schemaVersion, formDefinitionVersion, typeSlug, locale, capturedAt, title, sections, attestation);
	}

	@Override
	public String toString() {
		return "FormSnapshot{schemaVersion='" + schemaVersion + "', formDefinitionVersion='" + formDefinitionVersion + "', typeSlug='"
			+ typeSlug + "', locale='" + locale + "', capturedAt=" + capturedAt + ", title='" + title + "', sections=" + sections
			+ ", attestation=" + attestation + "}";
	}
}
