package se.sundsvall.caremanagement.formsnapshot.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * A section (step / group) of the form as it was rendered, carrying its own heading and any section-level description,
 * plus the fields it contained in render order.
 */
@Schema(description = "A section of the form as it was rendered.")
public class FormSnapshotSection {

	@Schema(description = "A stable section id", examples = "household")
	private String id;

	@Schema(description = "The section title the applicant saw", examples = "Hushåll")
	private String title;

	@Schema(description = "The section-level description / info text, if any", examples = "Uppgifter om din familj")
	private String description;

	@Schema(description = "Whether the section was visible to the applicant", examples = "true")
	private boolean visible;

	@ArraySchema(arraySchema = @Schema(description = "The fields in the section, in render order"), schema = @Schema(implementation = FormSnapshotField.class))
	private List<FormSnapshotField> fields;

	public static FormSnapshotSection create() {
		return new FormSnapshotSection();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public FormSnapshotSection withId(final String id) {
		this.id = id;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public FormSnapshotSection withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public FormSnapshotSection withDescription(final String description) {
		this.description = description;
		return this;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(final boolean visible) {
		this.visible = visible;
	}

	public FormSnapshotSection withVisible(final boolean visible) {
		this.visible = visible;
		return this;
	}

	public List<FormSnapshotField> getFields() {
		return fields;
	}

	public void setFields(final List<FormSnapshotField> fields) {
		this.fields = fields;
	}

	public FormSnapshotSection withFields(final List<FormSnapshotField> fields) {
		this.fields = fields;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FormSnapshotSection that = (FormSnapshotSection) o;
		return visible == that.visible && Objects.equals(id, that.id) && Objects.equals(title, that.title)
			&& Objects.equals(description, that.description) && Objects.equals(fields, that.fields);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, title, description, visible, fields);
	}

	@Override
	public String toString() {
		return "FormSnapshotSection{id='" + id + "', title='" + title + "', description='" + description + "', visible=" + visible
			+ ", fields=" + fields + "}";
	}
}
