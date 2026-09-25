package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * What careM knows about one income of a normberäkning saved in Lifecare — one Lifecare income type on one side
 * (applicant or co-applicant). It keeps the latest amount SSBTEK gave for it (written by the daily prepare) apart from
 * the amount the system last wrote to Lifecare (the proposal the prepare step created, or a change Draken's BFF applied
 * and acknowledged). A Lifecare value that still equals the system-written amount was not touched by a caseworker, so
 * a new SSBTEK amount may be written over it without asking; any other value was, and must be confirmed.
 */
@Entity
@Table(name = "errand_fa_calculation_sync",
	indexes = {
		@Index(name = "idx_fa_calculation_sync_errand", columnList = "errand_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_fa_calculation_sync_errand_type_role", columnNames = {
			"errand_id", "income_type_key", "role"
		})
	})
public class FaCalculationSyncEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false)
	private String errandId;

	/** The Lifecare income type name, normalized (trimmed, lower case) — what a Lifecare calculation row is matched on. */
	@Column(name = "income_type_key", nullable = false)
	private String incomeTypeKey;

	@Column(name = "income_type_id")
	private Integer incomeTypeId;

	@Column(name = "income_type_name")
	private String incomeTypeName;

	@Column(name = "role", nullable = false, length = 16)
	private String role;

	@Column(name = "ssbtek_amount", precision = 12, scale = 2)
	private BigDecimal ssbtekAmount;

	@Column(name = "ssbtek_read_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime ssbtekReadAt;

	@Column(name = "system_written_amount", precision = 12, scale = 2)
	private BigDecimal systemWrittenAmount;

	/** Set whenever the system wrote this income to Lifecare — also when it wrote it away, leaving the amount null. */
	@Column(name = "system_written_at")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime systemWrittenAt;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "updated")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime updated;

	public static FaCalculationSyncEntity create() {
		return new FaCalculationSyncEntity();
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

	public FaCalculationSyncEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public FaCalculationSyncEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getIncomeTypeKey() {
		return incomeTypeKey;
	}

	public void setIncomeTypeKey(final String incomeTypeKey) {
		this.incomeTypeKey = incomeTypeKey;
	}

	public FaCalculationSyncEntity withIncomeTypeKey(final String incomeTypeKey) {
		this.incomeTypeKey = incomeTypeKey;
		return this;
	}

	public Integer getIncomeTypeId() {
		return incomeTypeId;
	}

	public void setIncomeTypeId(final Integer incomeTypeId) {
		this.incomeTypeId = incomeTypeId;
	}

	public FaCalculationSyncEntity withIncomeTypeId(final Integer incomeTypeId) {
		this.incomeTypeId = incomeTypeId;
		return this;
	}

	public String getIncomeTypeName() {
		return incomeTypeName;
	}

	public void setIncomeTypeName(final String incomeTypeName) {
		this.incomeTypeName = incomeTypeName;
	}

	public FaCalculationSyncEntity withIncomeTypeName(final String incomeTypeName) {
		this.incomeTypeName = incomeTypeName;
		return this;
	}

	public String getRole() {
		return role;
	}

	public void setRole(final String role) {
		this.role = role;
	}

	public FaCalculationSyncEntity withRole(final String role) {
		this.role = role;
		return this;
	}

	public BigDecimal getSsbtekAmount() {
		return ssbtekAmount;
	}

	public void setSsbtekAmount(final BigDecimal ssbtekAmount) {
		this.ssbtekAmount = ssbtekAmount;
	}

	public FaCalculationSyncEntity withSsbtekAmount(final BigDecimal ssbtekAmount) {
		this.ssbtekAmount = ssbtekAmount;
		return this;
	}

	public OffsetDateTime getSsbtekReadAt() {
		return ssbtekReadAt;
	}

	public void setSsbtekReadAt(final OffsetDateTime ssbtekReadAt) {
		this.ssbtekReadAt = ssbtekReadAt;
	}

	public FaCalculationSyncEntity withSsbtekReadAt(final OffsetDateTime ssbtekReadAt) {
		this.ssbtekReadAt = ssbtekReadAt;
		return this;
	}

	public BigDecimal getSystemWrittenAmount() {
		return systemWrittenAmount;
	}

	public void setSystemWrittenAmount(final BigDecimal systemWrittenAmount) {
		this.systemWrittenAmount = systemWrittenAmount;
	}

	public FaCalculationSyncEntity withSystemWrittenAmount(final BigDecimal systemWrittenAmount) {
		this.systemWrittenAmount = systemWrittenAmount;
		return this;
	}

	public OffsetDateTime getSystemWrittenAt() {
		return systemWrittenAt;
	}

	public void setSystemWrittenAt(final OffsetDateTime systemWrittenAt) {
		this.systemWrittenAt = systemWrittenAt;
	}

	public FaCalculationSyncEntity withSystemWrittenAt(final OffsetDateTime systemWrittenAt) {
		this.systemWrittenAt = systemWrittenAt;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public FaCalculationSyncEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(final OffsetDateTime updated) {
		this.updated = updated;
	}

	public FaCalculationSyncEntity withUpdated(final OffsetDateTime updated) {
		this.updated = updated;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaCalculationSyncEntity that = (FaCalculationSyncEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId) && Objects.equals(incomeTypeKey, that.incomeTypeKey)
			&& Objects.equals(incomeTypeId, that.incomeTypeId) && Objects.equals(incomeTypeName, that.incomeTypeName) && Objects.equals(role, that.role)
			&& Objects.equals(ssbtekAmount, that.ssbtekAmount) && Objects.equals(ssbtekReadAt, that.ssbtekReadAt)
			&& Objects.equals(systemWrittenAmount, that.systemWrittenAmount) && Objects.equals(systemWrittenAt, that.systemWrittenAt)
			&& Objects.equals(created, that.created) && Objects.equals(updated, that.updated);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, incomeTypeKey, incomeTypeId, incomeTypeName, role, ssbtekAmount, ssbtekReadAt, systemWrittenAmount, systemWrittenAt,
			created, updated);
	}

	@Override
	public String toString() {
		return "FaCalculationSyncEntity{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", incomeTypeKey='" + incomeTypeKey + '\'' +
			", incomeTypeId=" + incomeTypeId +
			", incomeTypeName='" + incomeTypeName + '\'' +
			", role='" + role + '\'' +
			", ssbtekAmount=" + ssbtekAmount +
			", ssbtekReadAt=" + ssbtekReadAt +
			", systemWrittenAmount=" + systemWrittenAmount +
			", systemWrittenAt=" + systemWrittenAt +
			", created=" + created +
			", updated=" + updated +
			'}';
	}
}
