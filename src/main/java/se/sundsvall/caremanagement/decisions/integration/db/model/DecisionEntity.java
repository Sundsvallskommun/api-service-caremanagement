package se.sundsvall.caremanagement.decisions.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "decision")
public class DecisionEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "decision_type")
	private String decisionType;

	@Column(name = "value")
	private String value;

	@Column(name = "description", length = 4096)
	private String description;

	@Column(name = "amount", precision = 15, scale = 2)
	private BigDecimal amount;

	@Column(name = "decision_message", length = 8192)
	private String decisionMessage;

	@Column(name = "decision_date")
	private LocalDate decisionDate;

	@Column(name = "period_from")
	private LocalDate periodFrom;

	@Column(name = "period_to")
	private LocalDate periodTo;

	@Column(name = "created_by")
	private String createdBy;

	/**
	 * Where the decision stands in Lifecare: {@code PENDING} once it has been handed over to be written there
	 * (finalize), {@code SYNCED} once the writer reported it written, {@code FAILED} with Lifecare's own message in
	 * {@link #lifecareDetail}. Null for a decision that is never written to Lifecare (a recommendation, say).
	 */
	@Column(name = "lifecare_status", length = 16)
	private String lifecareStatus;

	@Column(name = "lifecare_id", length = 64)
	private String lifecareId;

	@Column(name = "lifecare_detail", length = 1024)
	private String lifecareDetail;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@PrePersist
	void prePersist() {
		if (created == null) {
			created = OffsetDateTime.now(ZoneId.systemDefault());
		}
	}

	public static DecisionEntity create() {
		return new DecisionEntity();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getDecisionType() {
		return decisionType;
	}

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getDecisionMessage() {
		return decisionMessage;
	}

	public LocalDate getDecisionDate() {
		return decisionDate;
	}

	public LocalDate getPeriodFrom() {
		return periodFrom;
	}

	public LocalDate getPeriodTo() {
		return periodTo;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public void setDecisionType(final String decisionType) {
		this.decisionType = decisionType;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public void setAmount(final BigDecimal amount) {
		this.amount = amount;
	}

	public void setDecisionMessage(final String decisionMessage) {
		this.decisionMessage = decisionMessage;
	}

	public void setDecisionDate(final LocalDate decisionDate) {
		this.decisionDate = decisionDate;
	}

	public void setPeriodFrom(final LocalDate periodFrom) {
		this.periodFrom = periodFrom;
	}

	public void setPeriodTo(final LocalDate periodTo) {
		this.periodTo = periodTo;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public DecisionEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public DecisionEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public DecisionEntity withDecisionType(final String decisionType) {
		this.decisionType = decisionType;
		return this;
	}

	public DecisionEntity withValue(final String value) {
		this.value = value;
		return this;
	}

	public DecisionEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public DecisionEntity withAmount(final BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public DecisionEntity withDecisionMessage(final String decisionMessage) {
		this.decisionMessage = decisionMessage;
		return this;
	}

	public DecisionEntity withDecisionDate(final LocalDate decisionDate) {
		this.decisionDate = decisionDate;
		return this;
	}

	public DecisionEntity withPeriodFrom(final LocalDate periodFrom) {
		this.periodFrom = periodFrom;
		return this;
	}

	public DecisionEntity withPeriodTo(final LocalDate periodTo) {
		this.periodTo = periodTo;
		return this;
	}

	public DecisionEntity withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public DecisionEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public String getLifecareStatus() {
		return lifecareStatus;
	}

	public void setLifecareStatus(final String lifecareStatus) {
		this.lifecareStatus = lifecareStatus;
	}

	public DecisionEntity withLifecareStatus(final String lifecareStatus) {
		this.lifecareStatus = lifecareStatus;
		return this;
	}

	public String getLifecareId() {
		return lifecareId;
	}

	public void setLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
	}

	public DecisionEntity withLifecareId(final String lifecareId) {
		this.lifecareId = lifecareId;
		return this;
	}

	public String getLifecareDetail() {
		return lifecareDetail;
	}

	public void setLifecareDetail(final String lifecareDetail) {
		this.lifecareDetail = lifecareDetail;
	}

	public DecisionEntity withLifecareDetail(final String lifecareDetail) {
		this.lifecareDetail = lifecareDetail;
		return this;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof final DecisionEntity other))
			return false;
		return Objects.equals(id, other.id) && Objects.equals(errandId, other.errandId)
			&& Objects.equals(decisionType, other.decisionType) && Objects.equals(value, other.value)
			&& Objects.equals(description, other.description) && Objects.equals(amount, other.amount)
			&& Objects.equals(decisionMessage, other.decisionMessage) && Objects.equals(decisionDate, other.decisionDate)
			&& Objects.equals(periodFrom, other.periodFrom) && Objects.equals(periodTo, other.periodTo)
			&& Objects.equals(createdBy, other.createdBy) && Objects.equals(lifecareStatus, other.lifecareStatus)
			&& Objects.equals(lifecareId, other.lifecareId) && Objects.equals(lifecareDetail, other.lifecareDetail)
			&& Objects.equals(created, other.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, decisionType, value, description, amount, decisionMessage, decisionDate, periodFrom, periodTo, createdBy, lifecareStatus, lifecareId,
			lifecareDetail, created);
	}

	@Override
	public String toString() {
		return "DecisionEntity{id='" + id + "', errandId='" + errandId + "', decisionType='" + decisionType
			+ "', value='" + value + "', amount=" + amount + ", decisionDate=" + decisionDate
			+ ", periodFrom=" + periodFrom + ", periodTo=" + periodTo + ", createdBy='" + createdBy + "', lifecareStatus='" + lifecareStatus
			+ "', lifecareId='" + lifecareId + "', lifecareDetail='" + lifecareDetail + "', created=" + created + '}';
	}
}
