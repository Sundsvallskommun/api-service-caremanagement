package se.sundsvall.caremanagement.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import se.sundsvall.caremanagement.api.validation.groups.OnCreate;

@Schema(description = "ContactChannel model")
public class ContactChannel {

	@Schema(description = "The key of the contact channel", examples = "Email")
	@NotBlank(groups = OnCreate.class)
	private String key;

	@Schema(description = "The value of the contact channel", examples = "joe.doe@example.com")
	private String value;

	public static ContactChannel create() {
		return new ContactChannel();
	}

	public String getKey() {
		return key;
	}

	public void setKey(final String key) {
		this.key = key;
	}

	public ContactChannel withKey(final String key) {
		this.key = key;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public ContactChannel withValue(final String value) {
		this.value = value;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ContactChannel that = (ContactChannel) o;
		return Objects.equals(key, that.key) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public String toString() {
		return "ContactChannel{" +
			"key='" + key + '\'' +
			", value='" + value + '\'' +
			'}';
	}
}
