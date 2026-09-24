package se.sundsvall.caremanagement.usersettings.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import se.sundsvall.caremanagement.shared.Auditable;
import se.sundsvall.caremanagement.shared.AuditableListener;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "user_settings", uniqueConstraints = {
	@UniqueConstraint(name = "uq_user_settings_municipality_id_ad_account", columnNames = {
		"municipality_id", "ad_account"
	})
})
@EntityListeners(AuditableListener.class)
public class UserSettingsEntity implements Auditable {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "ad_account", nullable = false, length = 64)
	private String adAccount;

	@Column(name = "ssbtek_open_in_new_window", nullable = false)
	private Boolean ssbtekOpenInNewWindow;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static UserSettingsEntity create() {
		return new UserSettingsEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public UserSettingsEntity withId(final Long id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public UserSettingsEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getAdAccount() {
		return adAccount;
	}

	public void setAdAccount(final String adAccount) {
		this.adAccount = adAccount;
	}

	public UserSettingsEntity withAdAccount(final String adAccount) {
		this.adAccount = adAccount;
		return this;
	}

	public Boolean getSsbtekOpenInNewWindow() {
		return ssbtekOpenInNewWindow;
	}

	public void setSsbtekOpenInNewWindow(final Boolean ssbtekOpenInNewWindow) {
		this.ssbtekOpenInNewWindow = ssbtekOpenInNewWindow;
	}

	public UserSettingsEntity withSsbtekOpenInNewWindow(final Boolean ssbtekOpenInNewWindow) {
		this.ssbtekOpenInNewWindow = ssbtekOpenInNewWindow;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	@Override
	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public UserSettingsEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	@Override
	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public UserSettingsEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(adAccount, created, id, modified, municipalityId, ssbtekOpenInNewWindow);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final UserSettingsEntity other)) {
			return false;
		}
		return Objects.equals(adAccount, other.adAccount) && Objects.equals(created, other.created) && Objects.equals(id, other.id) && Objects.equals(modified, other.modified)
			&& Objects.equals(municipalityId, other.municipalityId) && Objects.equals(ssbtekOpenInNewWindow, other.ssbtekOpenInNewWindow);
	}

	@Override
	public String toString() {
		return "UserSettingsEntity [id=" + id + ", municipalityId=" + municipalityId + ", adAccount=" + adAccount + ", ssbtekOpenInNewWindow=" + ssbtekOpenInNewWindow + ", created=" + created + ", modified=" + modified + "]";
	}
}
