package se.sundsvall.caremanagement.statushistory.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "errand_status_history",
	indexes = {
		@Index(name = "idx_status_history_errand_id", columnList = "errand_id"),
		@Index(name = "idx_status_history_changed_at", columnList = "changed_at")
	})
public class StatusHistoryEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "from_status", length = 64)
	private String fromStatus;

	@Column(name = "to_status", nullable = false, length = 64)
	private String toStatus;

	@Column(name = "changed_by", length = 64)
	private String changedBy;

	@Column(name = "changed_at", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime changedAt;

	public static StatusHistoryEntity create() {
		return new StatusHistoryEntity();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getFromStatus() {
		return fromStatus;
	}

	public String getToStatus() {
		return toStatus;
	}

	public String getChangedBy() {
		return changedBy;
	}

	public OffsetDateTime getChangedAt() {
		return changedAt;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public void setFromStatus(final String fromStatus) {
		this.fromStatus = fromStatus;
	}

	public void setToStatus(final String toStatus) {
		this.toStatus = toStatus;
	}

	public void setChangedBy(final String changedBy) {
		this.changedBy = changedBy;
	}

	public void setChangedAt(final OffsetDateTime changedAt) {
		this.changedAt = changedAt;
	}

	public StatusHistoryEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public StatusHistoryEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public StatusHistoryEntity withFromStatus(final String fromStatus) {
		this.fromStatus = fromStatus;
		return this;
	}

	public StatusHistoryEntity withToStatus(final String toStatus) {
		this.toStatus = toStatus;
		return this;
	}

	public StatusHistoryEntity withChangedBy(final String changedBy) {
		this.changedBy = changedBy;
		return this;
	}

	public StatusHistoryEntity withChangedAt(final OffsetDateTime changedAt) {
		this.changedAt = changedAt;
		return this;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof final StatusHistoryEntity other))
			return false;
		return Objects.equals(id, other.id) && Objects.equals(errandId, other.errandId)
			&& Objects.equals(fromStatus, other.fromStatus) && Objects.equals(toStatus, other.toStatus)
			&& Objects.equals(changedBy, other.changedBy) && Objects.equals(changedAt, other.changedAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, fromStatus, toStatus, changedBy, changedAt);
	}

	@Override
	public String toString() {
		return "StatusHistoryEntity{id='" + id + "', errandId='" + errandId + "', fromStatus='" + fromStatus
			+ "', toStatus='" + toStatus + "', changedBy='" + changedBy + "', changedAt=" + changedAt + '}';
	}
}
