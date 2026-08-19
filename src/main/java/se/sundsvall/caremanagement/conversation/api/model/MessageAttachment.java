package se.sundsvall.caremanagement.conversation.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Schema(description = """
	Metadata of a file attached to a message. Download the content via \
	GET .../messages/{messageId}/attachments/{id}/file""")
public class MessageAttachment {

	@Schema(description = "Unique identifier", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "File name", examples = "certificate.pdf")
	private String fileName;

	@Schema(description = "Mime type", examples = "application/pdf")
	private String mimeType;

	@Schema(description = "File size in bytes", examples = "1024", accessMode = READ_ONLY)
	private Integer fileSize;

	@Schema(description = """
		Who sent the file, derived from the message direction: CLIENT (applicant, INBOUND) or \
		CASEWORKER (caseworker, OUTBOUND)""", allowableValues = {
		"CLIENT", "CASEWORKER"
	}, examples = "CLIENT", accessMode = READ_ONLY)
	private String senderRole;

	@Schema(description = "Created timestamp", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	public static MessageAttachment create() {
		return new MessageAttachment();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public MessageAttachment withId(final String id) {
		this.id = id;
		return this;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public MessageAttachment withFileName(final String fileName) {
		this.fileName = fileName;
		return this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	public MessageAttachment withMimeType(final String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public Integer getFileSize() {
		return fileSize;
	}

	public void setFileSize(final Integer fileSize) {
		this.fileSize = fileSize;
	}

	public MessageAttachment withFileSize(final Integer fileSize) {
		this.fileSize = fileSize;
		return this;
	}

	public String getSenderRole() {
		return senderRole;
	}

	public void setSenderRole(final String senderRole) {
		this.senderRole = senderRole;
	}

	public MessageAttachment withSenderRole(final String senderRole) {
		this.senderRole = senderRole;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public MessageAttachment withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final MessageAttachment that = (MessageAttachment) o;
		return Objects.equals(id, that.id) && Objects.equals(fileName, that.fileName) && Objects.equals(mimeType, that.mimeType)
			&& Objects.equals(fileSize, that.fileSize) && Objects.equals(senderRole, that.senderRole) && Objects.equals(created, that.created);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, fileName, mimeType, fileSize, senderRole, created);
	}

	@Override
	public String toString() {
		return "MessageAttachment{id='" + id + "', fileName='" + fileName + "', mimeType='" + mimeType
			+ "', fileSize=" + fileSize + ", senderRole='" + senderRole + "', created=" + created + '}';
	}
}
