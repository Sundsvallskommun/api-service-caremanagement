package se.sundsvall.caremanagement.notifications.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.caremanagement.core.api.validation.groups.OnCreate;
import se.sundsvall.caremanagement.core.api.validation.groups.OnUpdate;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationSubType;
import se.sundsvall.caremanagement.notifications.integration.db.model.NotificationType;
import se.sundsvall.dept44.common.validators.annotation.MemberOf;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Schema(description = "User-facing notification raised against an errand. Mutable: callers acknowledge (acknowledged=true) when the recipient has seen it; expired notifications are purged by a background job.")
public class Notification {

	@Schema(description = "Unique identifier", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String id;

	@Schema(description = "Id of the errand the notification belongs to (server-assigned from path)", examples = "b82bd8ac-1507-4d9a-958d-369261eecc15", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String errandId;

	@Schema(description = "User id of the recipient (the user who should see this notification)", examples = "jane01doe")
	@NotBlank(groups = OnCreate.class)
	private String ownerId;

	@Schema(description = "User or system id that produced the notification. Automatically acknowledged if equal to ownerId.", examples = "john02doe")
	private String createdBy;

	@Schema(description = "Notification type", examples = "CREATE", allowableValues = {
		"CREATE", "UPDATE", "DELETE"
	})
	@NotNull(groups = OnCreate.class)
	@MemberOf(value = NotificationType.class, groups = {
		OnCreate.class, OnUpdate.class
	}, nullable = true)
	private String type;

	@Schema(description = "Notification sub-type", examples = "ERRAND", allowableValues = {
		"ERRAND", "DECISION", "ATTACHMENT", "STAKEHOLDER", "PARAMETER", "SYSTEM"
	})
	@MemberOf(value = NotificationSubType.class, groups = {
		OnCreate.class, OnUpdate.class
	}, nullable = true)
	private String subType;

	@Schema(description = "Short human-readable description", examples = "New errand assigned to you")
	@NotBlank(groups = OnCreate.class)
	@Size(max = 512, groups = {
		OnCreate.class, OnUpdate.class
	})
	private String description;

	@Schema(description = "Optional longer content / body")
	@Size(max = 2048, groups = {
		OnCreate.class, OnUpdate.class
	})
	private String content;

	@Schema(description = "Acknowledgement state. On PATCH, null leaves the value unchanged; true/false sets it. The bulk-acknowledge endpoint flips this to true for every notification on an errand.", examples = "false")
	private Boolean acknowledged;

	@Schema(description = "Timestamp after which the notification is eligible for cleanup (server-assigned)", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	@Null(groups = OnCreate.class)
	private OffsetDateTime expires;

	@Schema(description = "Creation timestamp (server-assigned)", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	@Null(groups = OnCreate.class)
	private OffsetDateTime created;

	@Schema(description = "Last-modified timestamp (server-assigned)", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	@Null(groups = OnCreate.class)
	private OffsetDateTime modified;

	public static Notification create() {
		return new Notification();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Notification withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public Notification withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(final String ownerId) {
		this.ownerId = ownerId;
	}

	public Notification withOwnerId(final String ownerId) {
		this.ownerId = ownerId;
		return this;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	public Notification withCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Notification withType(final String type) {
		this.type = type;
		return this;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(final String subType) {
		this.subType = subType;
	}

	public Notification withSubType(final String subType) {
		this.subType = subType;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Notification withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getContent() {
		return content;
	}

	public void setContent(final String content) {
		this.content = content;
	}

	public Notification withContent(final String content) {
		this.content = content;
		return this;
	}

	public Boolean getAcknowledged() {
		return acknowledged;
	}

	public void setAcknowledged(final Boolean acknowledged) {
		this.acknowledged = acknowledged;
	}

	public Notification withAcknowledged(final Boolean acknowledged) {
		this.acknowledged = acknowledged;
		return this;
	}

	public OffsetDateTime getExpires() {
		return expires;
	}

	public void setExpires(final OffsetDateTime expires) {
		this.expires = expires;
	}

	public Notification withExpires(final OffsetDateTime expires) {
		this.expires = expires;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Notification withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Notification withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, ownerId, createdBy, type, subType, description, content, acknowledged, expires, created, modified);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof final Notification other)) {
			return false;
		}
		return Objects.equals(id, other.id)
			&& Objects.equals(errandId, other.errandId)
			&& Objects.equals(ownerId, other.ownerId)
			&& Objects.equals(createdBy, other.createdBy)
			&& Objects.equals(type, other.type)
			&& Objects.equals(subType, other.subType)
			&& Objects.equals(description, other.description)
			&& Objects.equals(content, other.content)
			&& Objects.equals(acknowledged, other.acknowledged)
			&& Objects.equals(expires, other.expires)
			&& Objects.equals(created, other.created)
			&& Objects.equals(modified, other.modified);
	}

	@Override
	public String toString() {
		return "Notification{" +
			"id='" + id + '\'' +
			", errandId='" + errandId + '\'' +
			", ownerId='" + ownerId + '\'' +
			", createdBy='" + createdBy + '\'' +
			", type='" + type + '\'' +
			", subType='" + subType + '\'' +
			", description='" + description + '\'' +
			", content='" + content + '\'' +
			", acknowledged=" + acknowledged +
			", expires=" + expires +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
