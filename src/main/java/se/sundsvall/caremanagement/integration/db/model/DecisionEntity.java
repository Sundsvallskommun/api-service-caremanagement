package se.sundsvall.caremanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "decision")
public class DecisionEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_decision_errand_id"))
	private ErrandEntity errandEntity;

	@Column(name = "decision_type")
	private String decisionType;

	@Column(name = "value")
	private String value;

	@Column(name = "description", length = 4096)
	private String description;

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

	public void setId(final String id) {
		this.id = id;
	}

	public DecisionEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public DecisionEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public String getDecisionType() {
		return decisionType;
	}

	public void setDecisionType(final String decisionType) {
		this.decisionType = decisionType;
	}

	public DecisionEntity withDecisionType(final String decisionType) {
		this.decisionType = decisionType;
		return this;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public DecisionEntity withValue(final String value) {
		this.value = value;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public DecisionEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public DecisionEntity withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public DecisionEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	// errandEntity intentionally excluded from equals/hashCode/toString to avoid infinite recursion (bidirectional
	// relationship).
	@Override
	public int hashCode() {
		return Objects.hash(id, decisionType, value, description, createdBy, created);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final DecisionEntity other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(decisionType, other.decisionType)
			&& Objects.equals(value, other.value)
			&& Objects.equals(description, other.description)
			&& Objects.equals(createdBy, other.createdBy)
			&& Objects.equals(created, other.created);
	}

	@Override
	public String toString() {
		return "DecisionEntity{" +
			"id='" + id + '\'' +
			", errandEntity=" + errandEntity +
			", decisionType='" + decisionType + '\'' +
			", value='" + value + '\'' +
			", description='" + description + '\'' +
			", createdBy='" + createdBy + '\'' +
			", created=" + created +
			'}';
	}
}
