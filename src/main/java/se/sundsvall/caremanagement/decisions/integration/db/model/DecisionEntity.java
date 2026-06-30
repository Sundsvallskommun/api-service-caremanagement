package se.sundsvall.caremanagement.decisions.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "decision",
	indexes = {
		@Index(name = "idx_decision_errand_id", columnList = "errand_id")
	})
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

	@Column(name = "created")
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
			&& Objects.equals(createdBy, other.createdBy) && Objects.equals(created, other.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, decisionType, value, description, amount, decisionMessage, decisionDate, periodFrom, periodTo, createdBy, created);
	}

	@Override
	public String toString() {
		return "DecisionEntity{id='" + id + "', errandId='" + errandId + "', decisionType='" + decisionType
			+ "', value='" + value + "', amount=" + amount + ", decisionDate=" + decisionDate
			+ ", periodFrom=" + periodFrom + ", periodTo=" + periodTo + ", createdBy='" + createdBy + "', created=" + created + '}';
	}
}
