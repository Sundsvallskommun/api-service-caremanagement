package se.sundsvall.caremanagement.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.util.List;
import java.util.Objects;
import se.sundsvall.caremanagement.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "Parameter model")
public class Parameter {

	@Schema(description = "Unique identifier", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String id;

	@Schema(description = "Display name of the parameter", examples = "Priority level")
	private String displayName;

	@Schema(description = "Grouping for the parameter", examples = "contact")
	private String parameterGroup;

	@Schema(description = "Key of the parameter", examples = "priorityLevel")
	@NotBlank(groups = OnCreate.class)
	private String key;

	@Schema(description = "Values of the parameter")
	private List<String> values;

	public static Parameter create() {
		return new Parameter();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Parameter withId(final String id) {
		this.id = id;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public Parameter withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getParameterGroup() {
		return parameterGroup;
	}

	public void setParameterGroup(final String parameterGroup) {
		this.parameterGroup = parameterGroup;
	}

	public Parameter withParameterGroup(final String parameterGroup) {
		this.parameterGroup = parameterGroup;
		return this;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public Parameter withKey(final String key) {
		this.key = key;
		return this;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(final List<String> values) {
		this.values = values;
	}

	public Parameter withValues(final List<String> values) {
		this.values = values;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Parameter that = (Parameter) o;
		return Objects.equals(id, that.id) && Objects.equals(displayName, that.displayName) && Objects.equals(parameterGroup, that.parameterGroup) && Objects.equals(key, that.key) && Objects.equals(values, that.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, displayName, parameterGroup, key, values);
	}

	@Override
	public String toString() {
		return "Parameter{" +
			"id='" + id + '\'' +
			", displayName='" + displayName + '\'' +
			", parameterGroup='" + parameterGroup + '\'' +
			", key='" + key + '\'' +
			", values=" + values +
			'}';
	}
}
