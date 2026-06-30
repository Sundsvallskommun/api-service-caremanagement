package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * A single EB income warning on an errand — an acknowledgeable object the caseworker reviews in Draken. The daily
 * prepare step reconciles these against the current SSBTEK picture (creating new ones, auto-closing resolved ones);
 * dedup is on {@code (errandId, type, sourceKey)}.
 */
@Entity
@Table(name = "errand_financial_assistance_warning", indexes = {
	@Index(name = "idx_fa_warning_errand", columnList = "errand_id"),
	@Index(name = "idx_fa_warning_dedup", columnList = "errand_id, type, source_key")
})
public class FaWarningEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id")
	private String errandId;

	@Column(name = "type")
	private String type;

	@Column(name = "source_key")
	private String sourceKey;

	@Column(name = "message", length = LONG32)
	private String message;

	@Column(name = "status")
	private String status;

	@Column(name = "auto_resolved")
	private boolean autoResolved;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "updated")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime updated;

	public static FaWarningEntity create() {
		return new FaWarningEntity();
	}

	@PrePersist
	void prePersist() {
		final var now = OffsetDateTime.now(ZoneId.systemDefault());
		created = now;
		updated = now;
	}

	@PreUpdate
	void preUpdate() {
		updated = OffsetDateTime.now(ZoneId.systemDefault());
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public FaWarningEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public FaWarningEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public FaWarningEntity withType(final String type) {
		this.type = type;
		return this;
	}

	public String getSourceKey() {
		return sourceKey;
	}

	public void setSourceKey(final String sourceKey) {
		this.sourceKey = sourceKey;
	}

	public FaWarningEntity withSourceKey(final String sourceKey) {
		this.sourceKey = sourceKey;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public FaWarningEntity withMessage(final String message) {
		this.message = message;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public FaWarningEntity withStatus(final String status) {
		this.status = status;
		return this;
	}

	public boolean isAutoResolved() {
		return autoResolved;
	}

	public void setAutoResolved(final boolean autoResolved) {
		this.autoResolved = autoResolved;
	}

	public FaWarningEntity withAutoResolved(final boolean autoResolved) {
		this.autoResolved = autoResolved;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public FaWarningEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public FaWarningEntity withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaWarningEntity that = (FaWarningEntity) o;
		return autoResolved == that.autoResolved && Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId) && Objects.equals(type, that.type)
			&& Objects.equals(sourceKey, that.sourceKey) && Objects.equals(message, that.message) && Objects.equals(status, that.status)
			&& Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, type, sourceKey, message, status, autoResolved, created, updated);
	}

	@Override
	public String toString() {
		return "FaWarningEntity{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", type='" + type + '\'' +
			", sourceKey='" + sourceKey + '\'' +
			", message='" + message + '\'' +
			", status='" + status + '\'' +
			", autoResolved=" + autoResolved +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
