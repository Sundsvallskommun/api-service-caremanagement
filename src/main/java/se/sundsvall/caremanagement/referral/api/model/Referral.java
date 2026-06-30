package se.sundsvall.caremanagement.referral.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.caremanagement.core.api.validation.groups.OnCreate;
import se.sundsvall.dept44.common.validators.annotation.OneOf;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * A referral/consultation sent on an errand to an external authority, and its response. Type-agnostic:
 * {@code authority}
 * is namespace-defined free text. {@code sentAt} defaults to today when omitted; {@code status} follows the lifecycle
 * SENT → RESPONDED.
 */
@Schema(description = "A referral/consultation on an errand, with the receiving authority, due date and status.")
public class Referral {

	@Schema(description = "Unique id", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String id;

	@Schema(description = "The receiving authority (namespace-defined)", examples = "ENVIRONMENTAL_OFFICE")
	@NotBlank(groups = OnCreate.class)
	@Size(max = 64)
	private String authority;

	@Schema(description = "Recipient (name/unit)", examples = "Environmental Office, Sundsvall")
	@Size(max = 255)
	private String recipient;

	@Schema(description = "Date the referral was sent. Defaults to today when omitted.", examples = "2026-06-03")
	@DateTimeFormat(iso = DATE)
	private LocalDate sentAt;

	@Schema(description = "Response due date", examples = "2026-07-01")
	@DateTimeFormat(iso = DATE)
	private LocalDate dueAt;

	@Schema(description = "Response to the referral", examples = "The authority has no objection.")
	@Size(max = 4096)
	private String responseText;

	@Schema(description = "Status", examples = "SENT", allowableValues = {
		"SENT", "RESPONDED"
	})
	@OneOf(value = {
		"SENT", "RESPONDED"
	}, nullable = true)
	private String status;

	@Schema(description = "Created", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private OffsetDateTime created;

	@Schema(description = "Modified", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private OffsetDateTime modified;

	public static Referral create() {
		return new Referral();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Referral withId(final String id) {
		this.id = id;
		return this;
	}

	public String getAuthority() {
		return authority;
	}

	public void setAuthority(final String authority) {
		this.authority = authority;
	}

	public Referral withAuthority(final String authority) {
		this.authority = authority;
		return this;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(final String recipient) {
		this.recipient = recipient;
	}

	public Referral withRecipient(final String recipient) {
		this.recipient = recipient;
		return this;
	}

	public LocalDate getSentAt() {
		return sentAt;
	}

	public void setSentAt(final LocalDate sentAt) {
		this.sentAt = sentAt;
	}

	public Referral withSentAt(final LocalDate sentAt) {
		this.sentAt = sentAt;
		return this;
	}

	public LocalDate getDueAt() {
		return dueAt;
	}

	public void setDueAt(final LocalDate dueAt) {
		this.dueAt = dueAt;
	}

	public Referral withDueAt(final LocalDate dueAt) {
		this.dueAt = dueAt;
		return this;
	}

	public String getResponseText() {
		return responseText;
	}

	public void setResponseText(final String responseText) {
		this.responseText = responseText;
	}

	public Referral withResponseText(final String responseText) {
		this.responseText = responseText;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public Referral withStatus(final String status) {
		this.status = status;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Referral withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Referral withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Referral that = (Referral) o;
		return Objects.equals(id, that.id) && Objects.equals(authority, that.authority) && Objects.equals(recipient, that.recipient)
			&& Objects.equals(sentAt, that.sentAt) && Objects.equals(dueAt, that.dueAt) && Objects.equals(responseText, that.responseText)
			&& Objects.equals(status, that.status) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, authority, recipient, sentAt, dueAt, responseText, status, created, modified);
	}

	@Override
	public String toString() {
		return "Referral{id='" + id + "', authority='" + authority + "', recipient='" + recipient + "', sentAt=" + sentAt
			+ ", dueAt=" + dueAt + ", responseText='" + responseText + "', status='" + status + "', created=" + created
			+ ", modified=" + modified + '}';
	}
}
