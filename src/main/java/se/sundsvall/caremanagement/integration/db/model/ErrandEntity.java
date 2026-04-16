package se.sundsvall.caremanagement.integration.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import static jakarta.persistence.CascadeType.ALL;
import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Optional.ofNullable;
import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

@Entity
@Table(name = "errand",
	indexes = {
		@Index(name = "idx_errand_id", columnList = "id"),
		@Index(name = "idx_errand_namespace", columnList = "namespace"),
		@Index(name = "idx_errand_municipality_id", columnList = "municipality_id"),
		@Index(name = "idx_errand_municipality_id_namespace_status", columnList = "municipality_id,namespace,status"),
		@Index(name = "idx_errand_municipality_id_namespace_category", columnList = "municipality_id,namespace,category"),
		@Index(name = "idx_errand_municipality_id_namespace_type", columnList = "municipality_id,namespace,type"),
		@Index(name = "idx_errand_municipality_id_namespace_assigned_user_id", columnList = "municipality_id,namespace,assigned_user_id"),
		@Index(name = "idx_errand_municipality_id_namespace_reporter_user_id", columnList = "municipality_id,namespace,reporter_user_id"),
		@Index(name = "idx_errand_municipality_id_namespace_status_touched", columnList = "municipality_id,namespace,status,touched"),
		@Index(name = "idx_errand_municipality_id_namespace_status_modified", columnList = "municipality_id,namespace,status,modified"),
		@Index(name = "idx_errand_municipality_id_namespace_created", columnList = "municipality_id,namespace,created"),
		@Index(name = "idx_errand_municipality_id_namespace_touched", columnList = "municipality_id,namespace,touched")
	})
@EntityListeners(AuditableListener.class)
public class ErrandEntity implements Auditable {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@ElementCollection
	@CollectionTable(name = "external_tag",
		indexes = {
			@Index(name = "idx_external_tag_errand_id", columnList = "errand_id"),
			@Index(name = "idx_external_tag_key", columnList = "\"key\""),
			@Index(name = "idx_external_tag_value", columnList = "\"value\"")
		},
		joinColumns = @JoinColumn(name = "errand_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_errand_external_tag_errand_id")),
		uniqueConstraints = @UniqueConstraint(name = "uq_external_tag_errand_id_key", columnNames = {
			"errand_id", "\"key\""
		}))
	private List<TagEmbeddable> externalTags;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "title")
	private String title;

	@Column(name = "category")
	private String category;

	@Column(name = "type", length = 128)
	private String type;

	@Column(name = "status", length = 64)
	private String status;

	@Column(name = "description", length = LONG32)
	private String description;

	@Column(name = "priority")
	private String priority;

	@Column(name = "reporter_user_id")
	private String reporterUserId;

	@Column(name = "assigned_user_id")
	private String assignedUserId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "contact_reason_id", foreignKey = @ForeignKey(name = "fk_errand_contact_reason_id"))
	private LookupEntity contactReason;

	@Column(name = "contact_reason_description", length = 4096)
	private String contactReasonDescription;

	@OneToMany(mappedBy = "errandEntity", cascade = ALL, orphanRemoval = true)
	@OrderBy("fileName")
	private List<AttachmentEntity> attachments;

	@OneToMany(mappedBy = "errandEntity", cascade = ALL, orphanRemoval = true)
	private List<StakeholderEntity> stakeholders;

	@OneToMany(mappedBy = "errandEntity", cascade = ALL, orphanRemoval = true)
	private List<ParameterEntity> parameters;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@Column(name = "touched")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime touched;

	public static ErrandEntity create() {
		return new ErrandEntity();
	}

	@PrePersist
	@PreUpdate
	void onCreateOrUpdate() {
		touched = now(systemDefault()).truncatedTo(MILLIS);
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public ErrandEntity withId(final String id) {
		this.id = id;
		return this;
	}

	public List<TagEmbeddable> getExternalTags() {
		return externalTags;
	}

	public void setExternalTags(final List<TagEmbeddable> externalTags) {
		this.externalTags = externalTags;
	}

	public ErrandEntity withExternalTags(final List<TagEmbeddable> externalTags) {
		this.externalTags = externalTags;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public ErrandEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public ErrandEntity withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public ErrandEntity withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	public ErrandEntity withCategory(final String category) {
		this.category = category;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public ErrandEntity withType(final String type) {
		this.type = type;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public ErrandEntity withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public ErrandEntity withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(final String priority) {
		this.priority = priority;
	}

	public ErrandEntity withPriority(final String priority) {
		this.priority = priority;
		return this;
	}

	public String getReporterUserId() {
		return reporterUserId;
	}

	public void setReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
	}

	public ErrandEntity withReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
		return this;
	}

	public String getAssignedUserId() {
		return assignedUserId;
	}

	public void setAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
	}

	public ErrandEntity withAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
		return this;
	}

	public LookupEntity getContactReason() {
		return contactReason;
	}

	public void setContactReason(final LookupEntity contactReason) {
		this.contactReason = contactReason;
	}

	public ErrandEntity withContactReason(final LookupEntity contactReason) {
		this.contactReason = contactReason;
		return this;
	}

	public String getContactReasonDescription() {
		return contactReasonDescription;
	}

	public void setContactReasonDescription(final String contactReasonDescription) {
		this.contactReasonDescription = contactReasonDescription;
	}

	public ErrandEntity withContactReasonDescription(final String contactReasonDescription) {
		this.contactReasonDescription = contactReasonDescription;
		return this;
	}

	public List<AttachmentEntity> getAttachments() {
		return attachments;
	}

	public void setAttachments(final List<AttachmentEntity> attachments) {
		this.attachments = attachments;
	}

	public ErrandEntity withAttachments(final List<AttachmentEntity> attachments) {
		this.attachments = attachments;
		return this;
	}

	public List<StakeholderEntity> getStakeholders() {
		return stakeholders;
	}

	public void setStakeholders(final List<StakeholderEntity> stakeholders) {
		this.stakeholders = stakeholders;
	}

	public ErrandEntity withStakeholders(final List<StakeholderEntity> stakeholders) {
		this.stakeholders = stakeholders;
		return this;
	}

	public List<ParameterEntity> getParameters() {
		return parameters;
	}

	public void setParameters(final List<ParameterEntity> parameters) {
		this.parameters = parameters;
	}

	public ErrandEntity withParameters(final List<ParameterEntity> parameters) {
		this.parameters = parameters;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	@Override
	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public ErrandEntity withCreated(final OffsetDateTime created) {
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

	public ErrandEntity withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public OffsetDateTime getTouched() {
		return ofNullable(touched)
			.or(() -> ofNullable(modified).filter(modifiedAt -> created != null && modifiedAt.isAfter(created)))
			.orElse(created);
	}

	public void setTouched(final OffsetDateTime touched) {
		this.touched = touched;
	}

	public ErrandEntity withTouched(final OffsetDateTime touched) {
		this.touched = touched;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if ((o == null) || (getClass() != o.getClass())) {
			return false;
		}
		final ErrandEntity that = (ErrandEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(externalTags, that.externalTags) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(namespace, that.namespace) && Objects.equals(title, that.title)
			&& Objects.equals(category, that.category) && Objects.equals(type, that.type) && Objects.equals(status, that.status) && Objects.equals(description, that.description) && Objects.equals(priority, that.priority)
			&& Objects.equals(reporterUserId, that.reporterUserId) && Objects.equals(assignedUserId, that.assignedUserId) && Objects.equals(contactReason, that.contactReason) && Objects.equals(contactReasonDescription, that.contactReasonDescription)
			&& Objects.equals(attachments, that.attachments) && Objects.equals(stakeholders, that.stakeholders) && Objects.equals(parameters, that.parameters) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified)
			&& Objects.equals(touched, that.touched);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, externalTags, municipalityId, namespace, title, category, type, status, description, priority, reporterUserId, assignedUserId, contactReason, contactReasonDescription, attachments, stakeholders, parameters, created,
			modified,
			touched);
	}

	@Override
	public String toString() {
		return "ErrandEntity{" +
			"id='" + id + '\'' +
			", externalTags=" + externalTags +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", title='" + title + '\'' +
			", category='" + category + '\'' +
			", type='" + type + '\'' +
			", status='" + status + '\'' +
			", description='" + description + '\'' +
			", priority='" + priority + '\'' +
			", reporterUserId='" + reporterUserId + '\'' +
			", assignedUserId='" + assignedUserId + '\'' +
			", contactReason=" + contactReason +
			", contactReasonDescription='" + contactReasonDescription + '\'' +
			", attachments=" + attachments +
			", stakeholders=" + stakeholders +
			", parameters=" + parameters +
			", created=" + created +
			", modified=" + modified +
			", touched=" + touched +
			'}';
	}
}
