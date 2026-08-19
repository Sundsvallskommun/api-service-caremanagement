package se.sundsvall.caremanagement.formsnapshot.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * One repeated instance of a {@code REPEATING_GROUP} field (e.g. one income, one child) — the nested fields captured
 * for that instance, in render order. A named wrapper around the group's fields so the repeated structure is not an
 * anonymous nested list, and there is room for per-group metadata later.
 */
@Schema(description = "One repeated instance of a REPEATING_GROUP field — its nested fields, in render order.")
public class FormSnapshotGroup {

	@ArraySchema(arraySchema = @Schema(description = "The nested fields for this repeated instance, in render order"), schema = @Schema(implementation = FormSnapshotField.class))
	private List<FormSnapshotField> fields;

	public static FormSnapshotGroup create() {
		return new FormSnapshotGroup();
	}

	public List<FormSnapshotField> getFields() {
		return fields;
	}

	public void setFields(final List<FormSnapshotField> fields) {
		this.fields = fields;
	}

	public FormSnapshotGroup withFields(final List<FormSnapshotField> fields) {
		this.fields = fields;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FormSnapshotGroup that = (FormSnapshotGroup) o;
		return Objects.equals(fields, that.fields);
	}

	@Override
	public int hashCode() {
		return Objects.hash(fields);
	}

	@Override
	public String toString() {
		return "FormSnapshotGroup{fields=" + fields + "}";
	}
}
