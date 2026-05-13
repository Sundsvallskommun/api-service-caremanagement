package se.sundsvall.caremanagement.core.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.caremanagement.core.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

/**
 * Pure envelope. Sibling collections (stakeholders, decisions, attachments, notes,
 * status history) are owned by their respective modules and fetched via their own
 * endpoints — not embedded here.
 */
@Schema(description = "Errand envelope")
public class Errand {

	@Schema(description = "Unique identifier of the errand", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String id;

	@Schema(description = "Municipality id", examples = "2281", accessMode = READ_ONLY)
	private String municipalityId;

	@Schema(description = "Namespace", examples = "MY_NAMESPACE", accessMode = READ_ONLY)
	private String namespace;

	@Schema(description = "Human-readable errand number", examples = "CAREM-2026-00042", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String errandNumber;

	@Schema(description = "Registered errand type slug — validated against ErrandTypeRegistry", examples = "case-type-slug")
	@NotBlank(groups = OnCreate.class)
	private String typeSlug;

	@Schema(description = "Title for the errand", examples = "Title of the errand")
	@NotBlank(groups = OnCreate.class)
	private String title;

	@Schema(description = "Status of the errand", examples = "NEW")
	private String status;

	@Schema(description = "Description of the errand", examples = "Long description text")
	private String description;

	@Schema(description = "Priority of the errand", examples = "HIGH")
	private String priority;

	@Schema(description = "User id of the reporter", examples = "joe01doe")
	private String reporterUserId;

	@Schema(description = "User id of the assignee", examples = "jane02doe")
	private String assignedUserId;

	@Schema(description = "Name of the Operaton process definition to start when the errand is created", examples = "Handläggning av ärende")
	private String processDefinitionName;

	@Schema(description = "Id of the Operaton process instance started for this errand", examples = "a-process-instance-id", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String processInstanceId;

	@Schema(description = "Created timestamp", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime created;

	@Schema(description = "Modified timestamp", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime modified;

	@Schema(description = "Touched timestamp", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	private OffsetDateTime touched;

	public static Errand create() {
		return new Errand();
	}

	public String getId() {
		return id;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getErrandNumber() {
		return errandNumber;
	}

	public String getTypeSlug() {
		return typeSlug;
	}

	public String getTitle() {
		return title;
	}

	public String getStatus() {
		return status;
	}

	public String getDescription() {
		return description;
	}

	public String getPriority() {
		return priority;
	}

	public String getReporterUserId() {
		return reporterUserId;
	}

	public String getAssignedUserId() {
		return assignedUserId;
	}

	public String getProcessDefinitionName() {
		return processDefinitionName;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public OffsetDateTime getTouched() {
		return touched;
	}

	public void setId(final String v) {
		this.id = v;
	}

	public void setMunicipalityId(final String v) {
		this.municipalityId = v;
	}

	public void setNamespace(final String v) {
		this.namespace = v;
	}

	public void setErrandNumber(final String v) {
		this.errandNumber = v;
	}

	public void setTypeSlug(final String v) {
		this.typeSlug = v;
	}

	public void setTitle(final String v) {
		this.title = v;
	}

	public void setStatus(final String v) {
		this.status = v;
	}

	public void setDescription(final String v) {
		this.description = v;
	}

	public void setPriority(final String v) {
		this.priority = v;
	}

	public void setReporterUserId(final String v) {
		this.reporterUserId = v;
	}

	public void setAssignedUserId(final String v) {
		this.assignedUserId = v;
	}

	public void setProcessDefinitionName(final String v) {
		this.processDefinitionName = v;
	}

	public void setProcessInstanceId(final String v) {
		this.processInstanceId = v;
	}

	public void setCreated(final OffsetDateTime v) {
		this.created = v;
	}

	public void setModified(final OffsetDateTime v) {
		this.modified = v;
	}

	public void setTouched(final OffsetDateTime v) {
		this.touched = v;
	}

	public Errand withId(final String v) {
		this.id = v;
		return this;
	}

	public Errand withMunicipalityId(final String v) {
		this.municipalityId = v;
		return this;
	}

	public Errand withNamespace(final String v) {
		this.namespace = v;
		return this;
	}

	public Errand withErrandNumber(final String v) {
		this.errandNumber = v;
		return this;
	}

	public Errand withTypeSlug(final String v) {
		this.typeSlug = v;
		return this;
	}

	public Errand withTitle(final String v) {
		this.title = v;
		return this;
	}

	public Errand withStatus(final String v) {
		this.status = v;
		return this;
	}

	public Errand withDescription(final String v) {
		this.description = v;
		return this;
	}

	public Errand withPriority(final String v) {
		this.priority = v;
		return this;
	}

	public Errand withReporterUserId(final String v) {
		this.reporterUserId = v;
		return this;
	}

	public Errand withAssignedUserId(final String v) {
		this.assignedUserId = v;
		return this;
	}

	public Errand withProcessDefinitionName(final String v) {
		this.processDefinitionName = v;
		return this;
	}

	public Errand withProcessInstanceId(final String v) {
		this.processInstanceId = v;
		return this;
	}

	public Errand withCreated(final OffsetDateTime v) {
		this.created = v;
		return this;
	}

	public Errand withModified(final OffsetDateTime v) {
		this.modified = v;
		return this;
	}

	public Errand withTouched(final OffsetDateTime v) {
		this.touched = v;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Errand errand = (Errand) o;
		return Objects.equals(id, errand.id) && Objects.equals(municipalityId, errand.municipalityId)
			&& Objects.equals(namespace, errand.namespace) && Objects.equals(errandNumber, errand.errandNumber)
			&& Objects.equals(typeSlug, errand.typeSlug) && Objects.equals(title, errand.title)
			&& Objects.equals(status, errand.status) && Objects.equals(description, errand.description)
			&& Objects.equals(priority, errand.priority) && Objects.equals(reporterUserId, errand.reporterUserId)
			&& Objects.equals(assignedUserId, errand.assignedUserId)
			&& Objects.equals(processDefinitionName, errand.processDefinitionName)
			&& Objects.equals(processInstanceId, errand.processInstanceId)
			&& Objects.equals(created, errand.created) && Objects.equals(modified, errand.modified)
			&& Objects.equals(touched, errand.touched);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, errandNumber, typeSlug, title, status, description, priority,
			reporterUserId, assignedUserId, processDefinitionName, processInstanceId, created, modified, touched);
	}

	@Override
	public String toString() {
		return "Errand{id='" + id + "', municipalityId='" + municipalityId + "', namespace='" + namespace
			+ "', errandNumber='" + errandNumber + "', typeSlug='" + typeSlug + "', status='" + status
			+ "', title='" + title + "', description='" + description + "', priority='" + priority
			+ "', reporterUserId='" + reporterUserId + "', assignedUserId='" + assignedUserId
			+ "', processDefinitionName='" + processDefinitionName + "', processInstanceId='" + processInstanceId
			+ "', created=" + created + ", modified=" + modified + ", touched=" + touched + '}';
	}
}
