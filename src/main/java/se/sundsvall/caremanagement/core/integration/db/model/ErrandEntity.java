package se.sundsvall.caremanagement.core.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.caremanagement.shared.Auditable;
import se.sundsvall.caremanagement.shared.AuditableListener;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Optional.ofNullable;
import static org.hibernate.Length.LONG32;
import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

/**
 * Pure envelope. NO JPA relations to sibling modules — stakeholders, decisions, attachments, notes and status history
 * each own their own table with a plain {@code errand_id} FK and are queried by that id via their module's repository.
 */
@Entity
@Table(name = "errand",
	indexes = {
		@Index(name = "idx_errand_id", columnList = "id"),
		@Index(name = "idx_errand_namespace", columnList = "namespace"),
		@Index(name = "idx_errand_municipality_id", columnList = "municipality_id"),
		@Index(name = "idx_errand_errand_number", columnList = "errand_number"),
		@Index(name = "idx_errand_municipality_namespace_type_slug", columnList = "municipality_id,namespace,type_slug"),
		@Index(name = "idx_errand_municipality_namespace_status", columnList = "municipality_id,namespace,status"),
		@Index(name = "idx_errand_municipality_namespace_assigned_user_id", columnList = "municipality_id,namespace,assigned_user_id"),
		@Index(name = "idx_errand_municipality_namespace_reporter_user_id", columnList = "municipality_id,namespace,reporter_user_id"),
		@Index(name = "idx_errand_municipality_namespace_status_touched", columnList = "municipality_id,namespace,status,touched"),
		@Index(name = "idx_errand_municipality_namespace_created", columnList = "municipality_id,namespace,created"),
		@Index(name = "idx_errand_municipality_namespace_touched", columnList = "municipality_id,namespace,touched")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_errand_errand_number", columnNames = "errand_number")
	})
@EntityListeners(AuditableListener.class)
public class ErrandEntity implements Auditable {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 8)
	private String municipalityId;

	@Column(name = "namespace", nullable = false, length = 32)
	private String namespace;

	@Column(name = "errand_number", length = 64)
	private String errandNumber;

	@Column(name = "type_slug", length = 64)
	private String typeSlug;

	@Column(name = "title")
	private String title;

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

	@Column(name = "process_definition_name")
	private String processDefinitionName;

	@Column(name = "process_instance_id")
	private String processInstanceId;

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

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public String getErrandNumber() {
		return errandNumber;
	}

	public void setErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
	}

	public String getTypeSlug() {
		return typeSlug;
	}

	public void setTypeSlug(final String typeSlug) {
		this.typeSlug = typeSlug;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(final String priority) {
		this.priority = priority;
	}

	public String getReporterUserId() {
		return reporterUserId;
	}

	public void setReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
	}

	public String getAssignedUserId() {
		return assignedUserId;
	}

	public void setAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
	}

	public String getProcessDefinitionName() {
		return processDefinitionName;
	}

	public void setProcessDefinitionName(final String processDefinitionName) {
		this.processDefinitionName = processDefinitionName;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(final String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	@Override
	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	@Override
	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public OffsetDateTime getTouched() {
		return ofNullable(touched)
			.or(() -> ofNullable(modified).filter(modifiedAt -> created != null && modifiedAt.isAfter(created)))
			.orElse(created);
	}

	public void setTouched(final OffsetDateTime touched) {
		this.touched = touched;
	}

	public ErrandEntity withId(final String v) {
		this.id = v;
		return this;
	}

	public ErrandEntity withMunicipalityId(final String v) {
		this.municipalityId = v;
		return this;
	}

	public ErrandEntity withNamespace(final String v) {
		this.namespace = v;
		return this;
	}

	public ErrandEntity withErrandNumber(final String v) {
		this.errandNumber = v;
		return this;
	}

	public ErrandEntity withTypeSlug(final String v) {
		this.typeSlug = v;
		return this;
	}

	public ErrandEntity withTitle(final String v) {
		this.title = v;
		return this;
	}

	public ErrandEntity withStatus(final String v) {
		this.status = v;
		return this;
	}

	public ErrandEntity withDescription(final String v) {
		this.description = v;
		return this;
	}

	public ErrandEntity withPriority(final String v) {
		this.priority = v;
		return this;
	}

	public ErrandEntity withReporterUserId(final String v) {
		this.reporterUserId = v;
		return this;
	}

	public ErrandEntity withAssignedUserId(final String v) {
		this.assignedUserId = v;
		return this;
	}

	public ErrandEntity withProcessDefinitionName(final String v) {
		this.processDefinitionName = v;
		return this;
	}

	public ErrandEntity withProcessInstanceId(final String v) {
		this.processInstanceId = v;
		return this;
	}

	public ErrandEntity withCreated(final OffsetDateTime v) {
		this.created = v;
		return this;
	}

	public ErrandEntity withModified(final OffsetDateTime v) {
		this.modified = v;
		return this;
	}

	public ErrandEntity withTouched(final OffsetDateTime v) {
		this.touched = v;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final ErrandEntity that = (ErrandEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(municipalityId, that.municipalityId) && Objects.equals(namespace, that.namespace)
			&& Objects.equals(errandNumber, that.errandNumber) && Objects.equals(typeSlug, that.typeSlug) && Objects.equals(title, that.title)
			&& Objects.equals(status, that.status) && Objects.equals(description, that.description) && Objects.equals(priority, that.priority)
			&& Objects.equals(reporterUserId, that.reporterUserId) && Objects.equals(assignedUserId, that.assignedUserId)
			&& Objects.equals(processDefinitionName, that.processDefinitionName) && Objects.equals(processInstanceId, that.processInstanceId)
			&& Objects.equals(created, that.created) && Objects.equals(modified, that.modified) && Objects.equals(touched, that.touched);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, errandNumber, typeSlug, title, status, description, priority,
			reporterUserId, assignedUserId, processDefinitionName, processInstanceId, created, modified, touched);
	}

	@Override
	public String toString() {
		return "ErrandEntity{" +
			"id='" + id + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", namespace='" + namespace + '\'' +
			", errandNumber='" + errandNumber + '\'' +
			", typeSlug='" + typeSlug + '\'' +
			", title='" + title + '\'' +
			", status='" + status + '\'' +
			", description='" + description + '\'' +
			", priority='" + priority + '\'' +
			", reporterUserId='" + reporterUserId + '\'' +
			", assignedUserId='" + assignedUserId + '\'' +
			", processDefinitionName='" + processDefinitionName + '\'' +
			", processInstanceId='" + processInstanceId + '\'' +
			", created=" + created +
			", modified=" + modified +
			", touched=" + touched +
			'}';
	}
}
