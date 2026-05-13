package se.sundsvall.caremanagement.statushistory.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
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

	@Column(name = "errand_id", nullable = false, length = 255)
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

	public StatusHistoryEntity withId(final String v) {
		this.id = v;
		return this;
	}

	public StatusHistoryEntity withErrandId(final String v) {
		this.errandId = v;
		return this;
	}

	public StatusHistoryEntity withFromStatus(final String v) {
		this.fromStatus = v;
		return this;
	}

	public StatusHistoryEntity withToStatus(final String v) {
		this.toStatus = v;
		return this;
	}

	public StatusHistoryEntity withChangedBy(final String v) {
		this.changedBy = v;
		return this;
	}

	public StatusHistoryEntity withChangedAt(final OffsetDateTime v) {
		this.changedAt = v;
		return this;
	}
}
