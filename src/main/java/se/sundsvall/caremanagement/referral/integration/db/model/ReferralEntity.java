package se.sundsvall.caremanagement.referral.integration.db.model;

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
@Table(name = "referral",
	indexes = {
		@Index(name = "idx_referral_errand_id", columnList = "errand_id")
	})
@EntityListeners(AuditableListener.class)
public class ReferralEntity implements Auditable {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 36)
	private String errandId;

	@Column(name = "authority", length = 64)
	private String authority;

	@Column(name = "recipient", length = 32)
	private String recipient;

	@Column(name = "sent_at")
	private LocalDate sentAt;

	@Column(name = "due_at")
	private LocalDate dueAt;

	@Column(name = "response_text", length = 4096)
	private String responseText;

	@Column(name = "status", length = 32)
	private String status;

	@Column(name = "created")
	@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
	private OffsetDateTime modified;

	public static ReferralEntity create() {
		return new ReferralEntity();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getAuthority() {
		return authority;
	}

	public String getRecipient() {
		return recipient;
	}

	public LocalDate getSentAt() {
		return sentAt;
	}

	public LocalDate getDueAt() {
		return dueAt;
	}

	public String getResponseText() {
		return responseText;
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

	public void setAuthority(final String authority) {
		this.authority = authority;
	}

	public void setRecipient(final String recipient) {
		this.recipient = recipient;
	}

	public void setSentAt(final LocalDate sentAt) {
		this.sentAt = sentAt;
	}

	public void setDueAt(final LocalDate dueAt) {
		this.dueAt = dueAt;
	}

	public void setResponseText(final String responseText) {
		this.responseText = responseText;
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

	public ReferralEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public ReferralEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public ReferralEntity withAuthority(final String authority) {
		this.authority = authority;
		return this;
	}

	public ReferralEntity withRecipient(final String recipient) {
		this.recipient = recipient;
		return this;
	}

	public ReferralEntity withSentAt(final LocalDate sentAt) {
		this.sentAt = sentAt;
		return this;
	}

	public ReferralEntity withDueAt(final LocalDate dueAt) {
		this.dueAt = dueAt;
		return this;
	}

	public ReferralEntity withResponseText(final String responseText) {
		this.responseText = responseText;
		return this;
	}

	public ReferralEntity withStatus(final String status) {
		this.status = status;
		return this;
	}

	public ReferralEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public ReferralEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ReferralEntity that = (ReferralEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId) && Objects.equals(authority, that.authority)
			&& Objects.equals(recipient, that.recipient) && Objects.equals(sentAt, that.sentAt) && Objects.equals(dueAt, that.dueAt)
			&& Objects.equals(responseText, that.responseText) && Objects.equals(status, that.status)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, authority, recipient, sentAt, dueAt, responseText, status, created, modified);
	}

	@Override
	public String toString() {
		return "ReferralEntity{id='" + id + "', errandId='" + errandId + "', authority='" + authority + "', recipient='" + recipient
			+ "', sentAt=" + sentAt + ", dueAt=" + dueAt + ", responseText='" + responseText + "', status='" + status
			+ "', created=" + created + ", modified=" + modified + '}';
	}
}
