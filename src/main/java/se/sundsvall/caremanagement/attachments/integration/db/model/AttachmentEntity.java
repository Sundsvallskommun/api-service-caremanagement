package se.sundsvall.caremanagement.attachments.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.caremanagement.shared.Auditable;
import se.sundsvall.caremanagement.shared.AuditableListener;

import static jakarta.persistence.CascadeType.ALL;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "attachment",
	indexes = {
		@Index(name = "idx_attachment_file_name", columnList = "file_name"),
		@Index(name = "idx_attachment_municipality_id", columnList = "municipality_id"),
		@Index(name = "idx_attachment_namespace", columnList = "namespace"),
		@Index(name = "idx_attachment_errand_id", columnList = "errand_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_attachment_data_id", columnNames = "attachment_data_id")
	})
@EntityListeners(AuditableListener.class)
public class AttachmentEntity implements Auditable {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "errand_id", nullable = false, length = 255)
	private String errandId;

	@Column(name = "namespace", length = 32)
	private String namespace;

	@Column(name = "municipality_id", length = 8)
	private String municipalityId;

	@Column(name = "file_name")
	private String fileName;

	@Column(name = "mime_type")
	private String mimeType;

	@Column(name = "file_size")
	private Integer fileSize;

	@Column(name = "origin", length = 32)
	private String origin;

	@Column(name = "sender_role", length = 32)
	private String senderRole;

	@ManyToOne(fetch = FetchType.LAZY, cascade = ALL)
	@JoinColumn(name = "attachment_data_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attachment_data_attachment"))
	private AttachmentDataEntity attachmentData;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	public static AttachmentEntity create() {
		return new AttachmentEntity();
	}

	public String getId() {
		return id;
	}

	public String getErrandId() {
		return errandId;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public String getFileName() {
		return fileName;
	}

	public String getMimeType() {
		return mimeType;
	}

	public Integer getFileSize() {
		return fileSize;
	}

	public String getOrigin() {
		return origin;
	}

	public String getSenderRole() {
		return senderRole;
	}

	public AttachmentDataEntity getAttachmentData() {
		return attachmentData;
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

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	public void setFileSize(final Integer fileSize) {
		this.fileSize = fileSize;
	}

	public void setOrigin(final String origin) {
		this.origin = origin;
	}

	public void setSenderRole(final String senderRole) {
		this.senderRole = senderRole;
	}

	public void setAttachmentData(final AttachmentDataEntity attachmentData) {
		this.attachmentData = attachmentData;
	}

	@Override
	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	@Override
	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public AttachmentEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public AttachmentEntity withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public AttachmentEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public AttachmentEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public AttachmentEntity withFileName(final String fileName) {
		this.fileName = fileName;
		return this;
	}

	public AttachmentEntity withMimeType(final String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public AttachmentEntity withFileSize(final Integer fileSize) {
		this.fileSize = fileSize;
		return this;
	}

	public AttachmentEntity withOrigin(final String origin) {
		this.origin = origin;
		return this;
	}

	public AttachmentEntity withSenderRole(final String senderRole) {
		this.senderRole = senderRole;
		return this;
	}

	public AttachmentEntity withAttachmentData(final AttachmentDataEntity attachmentData) {
		this.attachmentData = attachmentData;
		return this;
	}

	public AttachmentEntity withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public AttachmentEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final AttachmentEntity that = (AttachmentEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(errandId, that.errandId)
			&& Objects.equals(namespace, that.namespace) && Objects.equals(municipalityId, that.municipalityId)
			&& Objects.equals(fileName, that.fileName) && Objects.equals(mimeType, that.mimeType)
			&& Objects.equals(fileSize, that.fileSize) && Objects.equals(origin, that.origin)
			&& Objects.equals(senderRole, that.senderRole) && Objects.equals(attachmentData, that.attachmentData)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandId, namespace, municipalityId, fileName, mimeType, fileSize, origin, senderRole, attachmentData, created, modified);
	}

	@Override
	public String toString() {
		return "AttachmentEntity{id='" + id + "', errandId='" + errandId + "', fileName='" + fileName
			+ "', mimeType='" + mimeType + "', fileSize=" + fileSize + ", origin='" + origin + "', senderRole='" + senderRole
			+ "', created=" + created + ", modified=" + modified + '}';
	}
}
