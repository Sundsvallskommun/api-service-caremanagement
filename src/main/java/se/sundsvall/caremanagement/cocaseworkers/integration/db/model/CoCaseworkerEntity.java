package se.sundsvall.caremanagement.cocaseworkers.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * A co-caseworker (medhandläggare) on an errand — a user, in addition to the errand's own
 * {@code assignedUserId}, who should see the errand's notifications. Namespace and municipality id are
 * denormalized onto the row (rather than resolved via a join to {@code errand}) so that a lookup "which errands
 * is this user a co-caseworker on" — the query the {@code notifications} module runs to widen notification
 * visibility — can be indexed the same way {@code NotificationEntity.ownerId} already is.
 */
@Entity
@Table(name = "errand_co_caseworker",
	uniqueConstraints = @UniqueConstraint(name = "uc_co_caseworker_errand_id_user_id", columnNames = {
		"errand_id", "user_id"
	}),
	indexes = {
		@Index(name = "idx_co_caseworker_errand_id", columnList = "errand_id"),
		@Index(name = "idx_co_caseworker_mid_ns_user_id", columnList = "municipality_id,namespace,user_id")
	})
public class CoCaseworkerEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "user_id", nullable = false, length = 64)
	private String userId;

	@Column(name = "created", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	public static CoCaseworkerEntity create() {
		return new CoCaseworkerEntity();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getUserId() {
		return userId;
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

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public void setUserId(final String userId) {
		this.userId = userId;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public CoCaseworkerEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public CoCaseworkerEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public CoCaseworkerEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public CoCaseworkerEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public CoCaseworkerEntity withUserId(final String userId) {
		this.userId = userId;
		return this;
	}

	public CoCaseworkerEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof final CoCaseworkerEntity other))
			return false;
		return Objects.equals(id, other.id) && Objects.equals(errandId, other.errandId)
			&& Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace)
			&& Objects.equals(userId, other.userId) && Objects.equals(created, other.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, municipalityId, namespace, userId, created);
	}

	@Override
	public String toString() {
		return "CoCaseworkerEntity{id='" + id + "', errandId='" + errandId + "', municipalityId='" + municipalityId
			+ "', namespace='" + namespace + "', userId='" + userId + "', created=" + created + '}';
	}
}
