package se.sundsvall.caremanagement.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.caremanagement.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Schema(description = "NamespaceConfig model")
public class NamespaceConfig {

	@Schema(description = "Unique identifier", examples = "1", accessMode = READ_ONLY)
	private Long id;

	@Schema(description = "Display name of the namespace", examples = "My namespace")
	@NotBlank(groups = OnCreate.class)
	private String displayName;

	@Schema(description = "Short code for the namespace", examples = "MY")
	private String shortCode;

	@Schema(description = "Created timestamp", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "Modified timestamp", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime modified;

	public static NamespaceConfig create() {
		return new NamespaceConfig();
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public NamespaceConfig withId(final Long id) {
		this.id = id;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public NamespaceConfig withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public String getShortCode() {
		return shortCode;
	}

	public void setShortCode(final String shortCode) {
		this.shortCode = shortCode;
	}

	public NamespaceConfig withShortCode(final String shortCode) {
		this.shortCode = shortCode;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public NamespaceConfig withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public NamespaceConfig withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NamespaceConfig that = (NamespaceConfig) o;
		return Objects.equals(id, that.id) && Objects.equals(displayName, that.displayName) && Objects.equals(shortCode, that.shortCode)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, displayName, shortCode, created, modified);
	}

	@Override
	public String toString() {
		return "NamespaceConfig{" +
			"id=" + id +
			", displayName='" + displayName + '\'' +
			", shortCode='" + shortCode + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
