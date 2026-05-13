package se.sundsvall.caremanagement.notifications.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.caremanagement.shared.Auditable;
import se.sundsvall.caremanagement.shared.AuditableListener;

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

	@Column(name = "errand_id", nullable = false, length = 255)
	private String errandId;

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

	public String getErrandId() {
		return errandId;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public NotificationType getType() {
		return type;
	}

	public NotificationSubType getSubType() {
		return subType;
	}

	public String getDescription() {
		return description;
	}

	public String getContent() {
		return content;
	}

	public boolean isAcknowledged() {
		return acknowledged;
	}

	public OffsetDateTime getExpires() {
		return expires;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setId(final String v) {
		this.id = v;
	}

	public void setErrandId(final String v) {
		this.errandId = v;
	}

	public void setMunicipalityId(final String v) {
		this.municipalityId = v;
	}

	public void setNamespace(final String v) {
		this.namespace = v;
	}

	public void setOwnerId(final String v) {
		this.ownerId = v;
	}

	public void setCreatedBy(final String v) {
		this.createdBy = v;
	}

	public void setType(final NotificationType v) {
		this.type = v;
	}

	public void setSubType(final NotificationSubType v) {
		this.subType = v;
	}

	public void setDescription(final String v) {
		this.description = v;
	}

	public void setContent(final String v) {
		this.content = v;
	}

	public void setAcknowledged(final boolean v) {
		this.acknowledged = v;
	}

	public void setExpires(final OffsetDateTime v) {
		this.expires = v;
	}

	@Override
	public void setCreated(final OffsetDateTime v) {
		this.created = v;
	}

	@Override
	public void setModified(final OffsetDateTime v) {
		this.modified = v;
	}

	public NotificationEntity withId(final String v) {
		this.id = v;
		return this;
	}

	public NotificationEntity withErrandId(final String v) {
		this.errandId = v;
		return this;
	}

	public NotificationEntity withMunicipalityId(final String v) {
		this.municipalityId = v;
		return this;
	}

	public NotificationEntity withNamespace(final String v) {
		this.namespace = v;
		return this;
	}

	public NotificationEntity withOwnerId(final String v) {
		this.ownerId = v;
		return this;
	}

	public NotificationEntity withCreatedBy(final String v) {
		this.createdBy = v;
		return this;
	}

	public NotificationEntity withType(final NotificationType v) {
		this.type = v;
		return this;
	}

	public NotificationEntity withSubType(final NotificationSubType v) {
		this.subType = v;
		return this;
	}

	public NotificationEntity withDescription(final String v) {
		this.description = v;
		return this;
	}

	public NotificationEntity withContent(final String v) {
		this.content = v;
		return this;
	}

	public NotificationEntity withAcknowledged(final boolean v) {
		this.acknowledged = v;
		return this;
	}

	public NotificationEntity withExpires(final OffsetDateTime v) {
		this.expires = v;
		return this;
	}

	public NotificationEntity withCreated(final OffsetDateTime v) {
		this.created = v;
		return this;
	}

	public NotificationEntity withModified(final OffsetDateTime v) {
		this.modified = v;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, municipalityId, namespace, ownerId, createdBy, type, subType, description,
			content, acknowledged, expires, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof final NotificationEntity other))
			return false;
		return acknowledged == other.acknowledged
			&& Objects.equals(id, other.id) && Objects.equals(errandId, other.errandId)
			&& Objects.equals(municipalityId, other.municipalityId) && Objects.equals(namespace, other.namespace)
			&& Objects.equals(ownerId, other.ownerId) && Objects.equals(createdBy, other.createdBy)
			&& type == other.type && subType == other.subType
			&& Objects.equals(description, other.description) && Objects.equals(content, other.content)
			&& Objects.equals(expires, other.expires) && Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "NotificationEntity{id='" + id + "', errandId='" + errandId + "', ownerId='" + ownerId
			+ "', type=" + type + ", subType=" + subType + ", description='" + description
			+ "', acknowledged=" + acknowledged + ", created=" + created + '}';
	}
}
