package se.sundsvall.caremanagement.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.caremanagement.api.validation.groups.OnCreate;

@Schema(description = "ExternalTag model")
public class ExternalTag {

	@Schema(description = "The key of the external tag", examples = "caseId")
	@NotBlank(groups = OnCreate.class)
	private String key;

	@Schema(description = "The value of the external tag", examples = "12345")
	private String value;

	public static ExternalTag create() {
		return new ExternalTag();
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public ExternalTag withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public ExternalTag withValue(final String value) {
		this.value = value;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ExternalTag that = (ExternalTag) o;
		return Objects.equals(key, that.key) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public String toString() {
		return "ExternalTag{" +
			"key='" + key + '\'' +
			", value='" + value + '\'' +
			'}';
	}
}
