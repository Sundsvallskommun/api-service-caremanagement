package se.sundsvall.caremanagement.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "notification",
	indexes = {
		@Index(name = "idx_notification_errand_id", columnList = "errand_id"),
		@Index(name = "idx_notification_mid_ns_owner_id_acknowledged", columnList = "municipality_id,namespace,owner_id,acknowledged"),
		@Index(name = "idx_notification_mid_ns_errand_id_acknowledged", columnList = "municipality_id,namespace,errand_id,acknowledged"),
		@Index(name = "idx_notification_expires", columnList = "expires")
	})
@EntityListeners(AuditableListener.class)
public class NotificationEntity implements Auditable {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "errand_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_errand_id"))
	private ErrandEntity errandEntity;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "owner_id", nullable = false)
	private String ownerId;

	@Column(name = "created_by")
	private String createdBy;

	@Column(name = "type", nullable = false, length = 32)
	@Enumerated(STRING)
	private NotificationType type;

	@Column(name = "sub_type", length = 32)
	@Enumerated(STRING)
	private NotificationSubType subType;

	@Column(name = "description", nullable = false, length = 512)
	private String description;

	@Column(name = "content", length = 2048)
	private String content;

	@Column(name = "acknowledged", nullable = false)
	private boolean acknowledged;

	@Column(name = "expires", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime expires;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static NotificationEntity create() {
		return new NotificationEntity();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public NotificationEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public ErrandEntity getErrandEntity() {
		return errandEntity;
	}

	public void setErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
	}

	public NotificationEntity withErrandEntity(final ErrandEntity errandEntity) {
		this.errandEntity = errandEntity;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public NotificationEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public NotificationEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(final String ownerId) {
		this.ownerId = ownerId;
	}

	public NotificationEntity withOwnerId(final String ownerId) {
		this.ownerId = ownerId;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public NotificationEntity withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public NotificationType getType() {
		return type;
	}

	public void setType(final NotificationType type) {
		this.type = type;
	}

	public NotificationEntity withType(final NotificationType type) {
		this.type = type;
		return this;
	}

	public NotificationSubType getSubType() {
		return subType;
	}

	public void setSubType(final NotificationSubType subType) {
		this.subType = subType;
	}

	public NotificationEntity withSubType(final NotificationSubType subType) {
		this.subType = subType;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public NotificationEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getContent() {
		return content;
	}

	public void setContent(final String content) {
		this.content = content;
	}

	public NotificationEntity withContent(final String content) {
		this.content = content;
		return this;
	}

	public boolean isAcknowledged() {
		return acknowledged;
	}

	public void setAcknowledged(final boolean acknowledged) {
		this.acknowledged = acknowledged;
	}

	public NotificationEntity withAcknowledged(final boolean acknowledged) {
		this.acknowledged = acknowledged;
		return this;
	}

	public OffsetDateTime getExpires() {
		return expires;
	}

	public void setExpires(final OffsetDateTime expires) {
		this.expires = expires;
	}

	public NotificationEntity withExpires(final OffsetDateTime expires) {
		this.expires = expires;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	@Override
	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public NotificationEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	@Override
	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public NotificationEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	// errandEntity intentionally excluded from equals/hashCode/toString to avoid infinite recursion (bidirectional
	// relationship).
	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, ownerId, createdBy, type, subType, description, content, acknowledged, expires, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final NotificationEntity other)) {
			return false;
		}
		return acknowledged == other.acknowledged
			&& Objects.equals(id, other.id)
			&& Objects.equals(municipalityId, other.municipalityId)
			&& Objects.equals(namespace, other.namespace)
			&& Objects.equals(ownerId, other.ownerId)
			&& Objects.equals(createdBy, other.createdBy)
			&& type == other.type
			&& subType == other.subType
			&& Objects.equals(description, other.description)
			&& Objects.equals(content, other.content)
			&& Objects.equals(expires, other.expires)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "NotificationEntity{" +
			"id='" + id + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", ownerId='" + ownerId + '\'' +
			", createdBy='" + createdBy + '\'' +
			", type=" + type +
			", subType=" + subType +
			", description='" + description + '\'' +
			", content='" + content + '\'' +
			", acknowledged=" + acknowledged +
			", expires=" + expires +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
