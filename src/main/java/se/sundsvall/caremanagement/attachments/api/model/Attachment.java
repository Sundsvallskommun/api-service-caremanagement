package se.sundsvall.caremanagement.attachments.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.caremanagement.core.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Schema(description = "Attachment model")
public class Attachment {

	@Schema(description = "Unique identifier", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "File name", examples = "my-file.pdf")
	@NotBlank(groups = OnCreate.class)
	private String fileName;

	@Schema(description = "Mime type", examples = "application/pdf")
	private String mimeType;

	@Schema(description = "File size in bytes", examples = "1024", accessMode = READ_ONLY)
	private Integer fileSize;

	@Schema(description = "Where the file came from: APPLICATION (citizen's application files), CONVERSATION (sent in a "
		+ "message thread), GENERATED (a consolidated PDF produced by the platform) or ERRAND (uploaded directly to the "
		+ "errand)", allowableValues = {
			"APPLICATION", "CONVERSATION", "GENERATED", "ERRAND"
	}, examples = "CONVERSATION", accessMode = READ_ONLY)
	private String origin;

	@Schema(description = "Who the file came from: CLIENT (applicant) or CASEWORKER (caseworker). May be null for "
		+ "files predating the distinction or with no clear sender.", allowableValues = {
			"CLIENT", "CASEWORKER"
	}, examples = "CLIENT", accessMode = READ_ONLY)
	private String senderRole;

	@Schema(description = "For CONVERSATION attachments, the id of the message the file is attached to — download it via "
		+ ".../messages/{messageId}/attachments/{id}/file. Null for non-conversation attachments, which download via "
		+ ".../attachments/{id}/file.", accessMode = READ_ONLY)
	private String messageId;

	@Schema(description = "Created timestamp", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "Modified timestamp", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime modified;

	public static Attachment create() {
		return new Attachment();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Attachment withId(final String id) {
		this.id = id;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public Attachment withFileName(final String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	public Attachment withMimeType(final String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public Integer getFileSize() {
		return fileSize;
	}

	public void setFileSize(final Integer fileSize) {
		this.fileSize = fileSize;
	}

	public Attachment withFileSize(final Integer fileSize) {
		this.fileSize = fileSize;
		return this;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(final String origin) {
		this.origin = origin;
	}

	public Attachment withOrigin(final String origin) {
		this.origin = origin;
		return this;
	}

	public String getSenderRole() {
		return senderRole;
	}

	public void setSenderRole(final String senderRole) {
		this.senderRole = senderRole;
	}

	public Attachment withSenderRole(final String senderRole) {
		this.senderRole = senderRole;
		return this;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(final String messageId) {
		this.messageId = messageId;
	}

	public Attachment withMessageId(final String messageId) {
		this.messageId = messageId;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Attachment withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Attachment withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Attachment that = (Attachment) o;
		return Objects.equals(id, that.id) && Objects.equals(fileName, that.fileName) && Objects.equals(mimeType, that.mimeType) && Objects.equals(fileSize, that.fileSize)
			&& Objects.equals(origin, that.origin) && Objects.equals(senderRole, that.senderRole) && Objects.equals(messageId, that.messageId)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, fileName, mimeType, fileSize, origin, senderRole, messageId, created, modified);
	}

	@Override
	public String toString() {
		return "Attachment{" +
			"id='" + id + '\'' +
			", fileName='" + fileName + '\'' +
			", mimeType='" + mimeType + '\'' +
			", fileSize=" + fileSize +
			", origin='" + origin + '\'' +
			", senderRole='" + senderRole + '\'' +
			", messageId='" + messageId + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}
