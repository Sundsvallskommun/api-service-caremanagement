package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * An EB income warning on an errand — an acknowledgeable object the caseworker reviews in Draken. Produced and
 * reconciled by the daily prepare step; a caseworker can acknowledge or close it.
 */
@Schema(description = "An EB income warning the caseworker can acknowledge or close.")
public class Warning {

	@Schema(description = "The warning id", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479", accessMode = Schema.AccessMode.READ_ONLY)
	private String id;

	@Schema(description = "The warning type", examples = "MISSING_SSBTEK", allowableValues = {
		"UNHANDLED_INCOME", "INCOME_CHANGE", "MISSING_SSBTEK", "NEW_INCOME"
	})
	private String type;

	@Schema(description = "A stable key for the income the warning concerns (benefit/incomeType) — the dedup key", examples = "Bostadsbidrag")
	private String sourceKey;

	@Schema(description = "Human-readable warning text", examples = "Still missing in SSBTEK: Dagersättning")
	private String message;

	@Schema(description = "The warning status", examples = "OPEN", allowableValues = {
		"OPEN", "ACKNOWLEDGED", "CLOSED"
	})
	private String status;

	@Schema(description = "Whether the warning was closed automatically (its cause resolved) rather than by a caseworker", examples = "false")
	private boolean autoResolved;

	@Schema(description = "When the warning was created", accessMode = Schema.AccessMode.READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "When the warning was last updated", accessMode = Schema.AccessMode.READ_ONLY)
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
		return autoResolved == warning.autoResolved && Objects.equals(id, warning.id) && Objects.equals(type, warning.type) && Objects.equals(sourceKey, warning.sourceKey)
			&& Objects.equals(message, warning.message) && Objects.equals(status, warning.status) && Objects.equals(created, warning.created) && Objects.equals(updated, warning.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, type, sourceKey, message, status, autoResolved, created, updated);
	}

	@Override
	public String toString() {
		return "Warning{" +
			"id='" + id + '\'' +
			", type='" + type + '\'' +
			", sourceKey='" + sourceKey + '\'' +
			", message='" + message + '\'' +
			", status='" + status + '\'' +
			", autoResolved=" + autoResolved +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
