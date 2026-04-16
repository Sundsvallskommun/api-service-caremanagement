package se.sundsvall.caremanagement.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.util.List;
import java.util.Objects;
import se.sundsvall.caremanagement.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "StakeholderParameter model")
public class StakeholderParameter {

	@Schema(description = "Unique identifier", examples = "42", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private Long id;

	@Schema(description = "Display name of the parameter", examples = "Phone number")
	private String displayName;

	@Schema(description = "Key of the parameter", examples = "phoneNumber")
	@NotBlank(groups = OnCreate.class)
	private String key;

	@Schema(description = "Values of the parameter")
	private List<String> values;

	public static StakeholderParameter create() {
		return new StakeholderParameter();
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public StakeholderParameter withId(final Long id) {
		this.id = id;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public StakeholderParameter withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public StakeholderParameter withKey(final String key) {
		this.key = key;
		return this;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(final List<String> values) {
		this.values = values;
	}

	public StakeholderParameter withValues(final List<String> values) {
		this.values = values;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final StakeholderParameter that = (StakeholderParameter) o;
		return Objects.equals(id, that.id) && Objects.equals(displayName, that.displayName) && Objects.equals(key, that.key) && Objects.equals(values, that.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, displayName, key, values);
	}

	@Override
	public String toString() {
		return "StakeholderParameter{" +
			"id=" + id +
			", displayName='" + displayName + '\'' +
			", key='" + key + '\'' +
			", values=" + values +
			'}';
	}
}
