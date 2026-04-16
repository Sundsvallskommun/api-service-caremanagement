package se.sundsvall.caremanagement.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.caremanagement.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Schema(description = "Lookup model - metadata entry (category, status, type, role, contact reason)")
public class Lookup {

	@Schema(description = "Name (machine-friendly key) of the lookup", examples = "NEW")
	@NotBlank(groups = OnCreate.class)
	private String name;

	@Schema(description = "Display name", examples = "New case")
	private String displayName;

	@Schema(description = "Created timestamp", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "Modified timestamp", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime modified;

	public static Lookup create() {
		return new Lookup();
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Lookup withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public Lookup withDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Lookup withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Lookup withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Lookup that = (Lookup) o;
		return Objects.equals(name, that.name) && Objects.equals(displayName, that.displayName) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, displayName, created, modified);
	}

	@Override
	public String toString() {
		return "Lookup{" +
			"name='" + name + '\'' +
			", displayName='" + displayName + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
