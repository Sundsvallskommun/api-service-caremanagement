package se.sundsvall.caremanagement.permit.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.caremanagement.shared.Auditable;
import se.sundsvall.caremanagement.shared.AuditableListener;

@Entity
@Table(name = "permit",
	indexes = {
		@Index(name = "idx_permit_errand_id", columnList = "errand_id")
	})
@EntityListeners(AuditableListener.class)
public class PermitEntity implements Auditable {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "permit_type", length = 64)
	private String permitType;

	@Column(name = "valid_from")
	private LocalDate validFrom;

	@Column(name = "valid_until")
	private LocalDate validUntil;

	@Column(name = "conditions", length = 4096)
	private String conditions;

	@Column(name = "status", length = 32)
	private String status;

	@Column(name = "created")
	@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
	private OffsetDateTime modified;

	public static PermitEntity create() {
		return new PermitEntity();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getPermitType() {
		return permitType;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public LocalDate getValidUntil() {
		return validUntil;
	}

	public String getConditions() {
		return conditions;
	}

	public String getStatus() {
		return status;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public void setPermitType(final String permitType) {
		this.permitType = permitType;
	}

	public void setValidFrom(final LocalDate validFrom) {
		this.validFrom = validFrom;
	}

	public void setValidUntil(final LocalDate validUntil) {
		this.validUntil = validUntil;
	}

	public void setConditions(final String conditions) {
		this.conditions = conditions;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	@Override
	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	@Override
	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public PermitEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public PermitEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public PermitEntity withPermitType(final String permitType) {
		this.permitType = permitType;
		return this;
	}

	public PermitEntity withValidFrom(final LocalDate validFrom) {
		this.validFrom = validFrom;
		return this;
	}

	public PermitEntity withValidUntil(final LocalDate validUntil) {
		this.validUntil = validUntil;
		return this;
	}

	public PermitEntity withConditions(final String conditions) {
		this.conditions = conditions;
		return this;
	}

	public PermitEntity withStatus(final String status) {
		this.status = status;
		return this;
	}

	public PermitEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public PermitEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PermitEntity that = (PermitEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId) && Objects.equals(permitType, that.permitType)
			&& Objects.equals(validFrom, that.validFrom) && Objects.equals(validUntil, that.validUntil)
			&& Objects.equals(conditions, that.conditions) && Objects.equals(status, that.status)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, permitType, validFrom, validUntil, conditions, status, created, modified);
	}

	@Override
	public String toString() {
		return "PermitEntity{id='" + id + "', errandId='" + errandId + "', permitType='" + permitType
			+ "', validFrom=" + validFrom + ", validUntil=" + validUntil + ", conditions='" + conditions
			+ "', status='" + status + "', created=" + created + ", modified=" + modified + '}';
	}
}
