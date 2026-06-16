package se.sundsvall.caremanagement.errandtypes.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * Describes a single field a type's {@code data} payload can carry, as form guidance for the frontend. The server
 * stores
 * the superset and does not enforce per-type requiredness — these descriptors say which fields a given application type
 * should collect, their kind, allowable enum values and any conditional gate.
 */
@Schema(description = "Form-guidance descriptor for a single data field of an errand type.")
public class FieldDescriptor {

	@Schema(description = "The data field name", examples = "maritalStatus")
	private String name;

	@Schema(description = "The field kind", examples = "ENUM", allowableValues = {
		"STRING", "BOOLEAN", "INTEGER", "DECIMAL", "DATE_TIME", "ENUM", "ARRAY"
	})
	private String type;

	@Schema(description = "True when the field is unconditionally required for the application types it applies to", examples = "true")
	private boolean required;

	@Schema(description = "Allowable values when type is ENUM, otherwise null", examples = "[\"SINGLE\",\"COHABITING\"]")
	private List<String> options;

	@Schema(description = "For ARRAY fields, the OpenAPI component name of the element shape (resolve via /api-docs); null otherwise", examples = "Cost")
	private String itemsRef;

	@Schema(description = "The application types that collect this field", examples = "[\"NEW\",\"RENEWAL\"]")
	private List<String> appliesTo;

	@Schema(description = "Human-readable gate describing when the field is collected, when conditional; null when always collected", examples = "periodChoice == OTHER_BENEFIT")
	private String condition;

	@Schema(description = "Short description of the field", examples = "Marital status of the applicant")
	private String description;

	public static FieldDescriptor create() {
		return new FieldDescriptor();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public FieldDescriptor withName(final String name) {
		this.name = name;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public FieldDescriptor withType(final String type) {
		this.type = type;
		return this;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(final boolean required) {
		this.required = required;
	}

	public FieldDescriptor withRequired(final boolean required) {
		this.required = required;
		return this;
	}

	public List<String> getOptions() {
		return options;
	}

	public void setOptions(final List<String> options) {
		this.options = options;
	}

	public FieldDescriptor withOptions(final List<String> options) {
		this.options = options;
		return this;
	}

	public String getItemsRef() {
		return itemsRef;
	}

	public void setItemsRef(final String itemsRef) {
		this.itemsRef = itemsRef;
	}

	public FieldDescriptor withItemsRef(final String itemsRef) {
		this.itemsRef = itemsRef;
		return this;
	}

	public List<String> getAppliesTo() {
		return appliesTo;
	}

	public void setAppliesTo(final List<String> appliesTo) {
		this.appliesTo = appliesTo;
	}

	public FieldDescriptor withAppliesTo(final List<String> appliesTo) {
		this.appliesTo = appliesTo;
		return this;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(final String condition) {
		this.condition = condition;
	}

	public FieldDescriptor withCondition(final String condition) {
		this.condition = condition;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public FieldDescriptor withDescription(final String description) {
		this.description = description;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FieldDescriptor that = (FieldDescriptor) o;
		return required == that.required && Objects.equals(name, that.name) && Objects.equals(type, that.type)
			&& Objects.equals(options, that.options) && Objects.equals(itemsRef, that.itemsRef)
			&& Objects.equals(appliesTo, that.appliesTo) && Objects.equals(condition, that.condition)
			&& Objects.equals(description, that.description);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type, required, options, itemsRef, appliesTo, condition, description);
	}

	@Override
	public String toString() {
		return "FieldDescriptor{name='" + name + "', type='" + type + "', required=" + required + ", options=" + options
			+ ", itemsRef='" + itemsRef + "', appliesTo=" + appliesTo + ", condition='" + condition + "', description='"
			+ description + "'}";
	}
}
