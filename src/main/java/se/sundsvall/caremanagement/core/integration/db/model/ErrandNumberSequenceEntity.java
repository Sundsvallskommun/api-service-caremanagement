package se.sundsvall.caremanagement.core.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

import static jakarta.persistence.GenerationType.IDENTITY;

/**
 * Running counter backing the human-readable errand number. One row per
 * {@code (municipalityId, namespace, sequenceYear)}; {@code currentValue} holds the last number handed out and is
 * incremented under a pessimistic write lock when an errand is created. The year is part of the key so the count
 * restarts every January.
 */
@Entity
@Table(name = "errand_number_sequence",
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_errand_number_sequence", columnNames = {
			"municipality_id", "namespace", "sequence_year"
		})
	})
public class ErrandNumberSequenceEntity {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "sequence_year", nullable = false)
	private Integer sequenceYear;

	@Column(name = "current_value", nullable = false)
	private Long currentValue;

	public static ErrandNumberSequenceEntity create() {
		return new ErrandNumberSequenceEntity();
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public Integer getSequenceYear() {
		return sequenceYear;
	}

	public void setSequenceYear(final Integer sequenceYear) {
		this.sequenceYear = sequenceYear;
	}

	public Long getCurrentValue() {
		return currentValue;
	}

	public void setCurrentValue(final Long currentValue) {
		this.currentValue = currentValue;
	}

	public ErrandNumberSequenceEntity withId(final Long v) {
		this.id = v;
		return this;
	}

	public ErrandNumberSequenceEntity withMunicipalityId(final String v) {
		this.municipalityId = v;
		return this;
	}

	public ErrandNumberSequenceEntity withNamespace(final String v) {
		this.namespace = v;
		return this;
	}

	public ErrandNumberSequenceEntity withSequenceYear(final Integer v) {
		this.sequenceYear = v;
		return this;
	}

	public ErrandNumberSequenceEntity withCurrentValue(final Long v) {
		this.currentValue = v;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final ErrandNumberSequenceEntity that = (ErrandNumberSequenceEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(namespace, that.namespace)
			&& Objects.equals(sequenceYear, that.sequenceYear) && Objects.equals(currentValue, that.currentValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, sequenceYear, currentValue);
	}

	@Override
	public String toString() {
		return "ErrandNumberSequenceEntity [id=" + id + ", municipalityId=" + municipalityId + ", namespace=" + namespace
			+ ", sequenceYear=" + sequenceYear + ", currentValue=" + currentValue + "]";
	}
}
