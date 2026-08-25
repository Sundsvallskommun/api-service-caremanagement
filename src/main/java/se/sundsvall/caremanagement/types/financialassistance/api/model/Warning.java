package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * A financial assistance income warning on an errand — an acknowledgeable object the caseworker reviews in Draken.
 * Produced and
 * reconciled by the daily prepare step; a caseworker can acknowledge or close it.
 */
@Schema(description = "A financial assistance income warning the caseworker can acknowledge or close.")
public class Warning {

	@Schema(description = "The warning id", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479", accessMode = Schema.AccessMode.READ_ONLY)
	private String id;

	@Schema(description = "The warning type (machine code; use typeDisplayName for the label)", examples = "EXPENSE_CAPPED", allowableValues = {
		"UNHANDLED_INCOME", "INCOME_CHANGE", "MISSING_SSBTEK", "NEW_INCOME", "NEW_EXPENSE", "NEW_PERSON",
		"INCOME_DROPPED", "HOUSEHOLD_CHANGE", "HOUSING_COST_CHANGE", "EXPENSE_REVIEW", "EXPENSE_CAPPED"
	})
	private String type;

	@Schema(description = "Swedish display name for the warning type", examples = "Kapad kostnad", accessMode = Schema.AccessMode.READ_ONLY)
	private String typeDisplayName;

	@Schema(description = "A stable key for the income the warning concerns (benefit/incomeType) — the dedup key", examples = "Bostadsbidrag")
	private String sourceKey;

	@Schema(description = "Human-readable warning text (Swedish)", examples = "Saknas fortfarande i SSBTEK: Dagersättning")
	private String message;

	@Schema(description = "The warning status (machine code; use statusDisplayName for the label)", examples = "OPEN", allowableValues = {
		"OPEN", "ACKNOWLEDGED", "CLOSED"
	})
	private String status;

	@Schema(description = "Swedish display name for the warning status", examples = "Öppen", accessMode = Schema.AccessMode.READ_ONLY)
	private String statusDisplayName;

	@Schema(description = "Whether the warning was closed automatically (its cause resolved) rather than by a caseworker", examples = "false")
	private boolean autoResolved;

	@Schema(description = "When the warning was created", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "When the warning was last updated", accessMode = Schema.AccessMode.READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime updated;

	public static Warning create() {
		return new Warning();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Warning withId(final String id) {
		this.id = id;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Warning withType(final String type) {
		this.type = type;
		return this;
	}

	public String getTypeDisplayName() {
		return typeDisplayName;
	}

	public void setTypeDisplayName(final String typeDisplayName) {
		this.typeDisplayName = typeDisplayName;
	}

	public Warning withTypeDisplayName(final String typeDisplayName) {
		this.typeDisplayName = typeDisplayName;
		return this;
	}

	public String getSourceKey() {
		return sourceKey;
	}

	public void setSourceKey(final String sourceKey) {
		this.sourceKey = sourceKey;
	}

	public Warning withSourceKey(final String sourceKey) {
		this.sourceKey = sourceKey;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public Warning withMessage(final String message) {
		this.message = message;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public Warning withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getStatusDisplayName() {
		return statusDisplayName;
	}

	public void setStatusDisplayName(final String statusDisplayName) {
		this.statusDisplayName = statusDisplayName;
	}

	public Warning withStatusDisplayName(final String statusDisplayName) {
		this.statusDisplayName = statusDisplayName;
		return this;
	}

	public boolean isAutoResolved() {
		return autoResolved;
	}

	public void setAutoResolved(final boolean autoResolved) {
		this.autoResolved = autoResolved;
	}

	public Warning withAutoResolved(final boolean autoResolved) {
		this.autoResolved = autoResolved;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Warning withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public Warning withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Warning warning = (Warning) o;
		return autoResolved == warning.autoResolved && Objects.equals(id, warning.id) && Objects.equals(type, warning.type) && Objects.equals(typeDisplayName, warning.typeDisplayName)
			&& Objects.equals(sourceKey, warning.sourceKey) && Objects.equals(message, warning.message) && Objects.equals(status, warning.status)
			&& Objects.equals(statusDisplayName, warning.statusDisplayName) && Objects.equals(created, warning.created) && Objects.equals(updated, warning.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, type, typeDisplayName, sourceKey, message, status, statusDisplayName, autoResolved, created, updated);
	}

	@Override
	public String toString() {
		return "Warning{" +
			"id='" + id + '\'' +
			", type='" + type + '\'' +
			", typeDisplayName='" + typeDisplayName + '\'' +
			", sourceKey='" + sourceKey + '\'' +
			", message='" + message + '\'' +
			", status='" + status + '\'' +
			", statusDisplayName='" + statusDisplayName + '\'' +
			", autoResolved=" + autoResolved +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
