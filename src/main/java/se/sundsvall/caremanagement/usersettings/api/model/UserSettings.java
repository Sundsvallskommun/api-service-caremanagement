package se.sundsvall.caremanagement.usersettings.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Schema(description = "Settings for one user (AD account). A user who has never saved any settings gets the defaults.")
public class UserSettings {

	@Schema(description = "The user's AD account (lower case)", examples = "joe01doe", accessMode = READ_ONLY)
	private String adAccount;

	@Schema(description = "Whether the SSBTEK view opens in a new window. Defaults to true.", examples = "true", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private Boolean ssbtekOpenInNewWindow;

	@Schema(description = "Created timestamp, null while the user runs on defaults", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "Modified timestamp, null while the user runs on defaults", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime modified;

	public static UserSettings create() {
		return new UserSettings();
	}

	public String getAdAccount() {
		return adAccount;
	}

	public void setAdAccount(final String adAccount) {
		this.adAccount = adAccount;
	}

	public UserSettings withAdAccount(final String adAccount) {
		this.adAccount = adAccount;
		return this;
	}

	public Boolean getSsbtekOpenInNewWindow() {
		return ssbtekOpenInNewWindow;
	}

	public void setSsbtekOpenInNewWindow(final Boolean ssbtekOpenInNewWindow) {
		this.ssbtekOpenInNewWindow = ssbtekOpenInNewWindow;
	}

	public UserSettings withSsbtekOpenInNewWindow(final Boolean ssbtekOpenInNewWindow) {
		this.ssbtekOpenInNewWindow = ssbtekOpenInNewWindow;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public UserSettings withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public UserSettings withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final UserSettings that = (UserSettings) o;
		return Objects.equals(adAccount, that.adAccount) && Objects.equals(ssbtekOpenInNewWindow, that.ssbtekOpenInNewWindow)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(adAccount, ssbtekOpenInNewWindow, created, modified);
	}

	@Override
	public String toString() {
		return "UserSettings{" +
			"adAccount='" + adAccount + '\'' +
			", ssbtekOpenInNewWindow=" + ssbtekOpenInNewWindow +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
