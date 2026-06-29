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
	@Null(groups = OnCreate.class)
	private String municipalityId;

	@Schema(description = "Namespace", examples = "MY_NAMESPACE", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String namespace;

	@Schema(description = "Human-readable errand number", examples = "EB-26060071", accessMode = READ_ONLY)
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

	@Schema(description = """
		Denormalized display name of the errand's applicant, maintained from the APPLICANT stakeholder. \
		Sortable and searchable on the errand list (e.g. ?sort=applicantName,asc). Null for errand types with no applicant.""",
		examples = "Anna Andersson",
		accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String applicantName;

	@Schema(
		description = "Name of the Operaton process definition associated with the errand. Recorded on the errand; the core create endpoint does not itself start a process — process start, when applicable, is handled per errand type by a type module reacting to the errand-created domain event.",
		examples = "Handläggning av ärende")
	private String processDefinitionName;

	@Schema(description = "Id of the Operaton process instance started for this errand", examples = "a-process-instance-id", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String processInstanceId;

	@Schema(description = "Created timestamp", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	@Null(groups = OnCreate.class)
	private OffsetDateTime created;

	@Schema(description = "Modified timestamp", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	@Null(groups = OnCreate.class)
	private OffsetDateTime modified;

	@Schema(description = "Touched timestamp", accessMode = READ_ONLY)
	@DateTimeFormat(iso = DATE_TIME)
	@Null(groups = OnCreate.class)
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

	public String getApplicantName() {
		return applicantName;
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

	public void setId(final String id) {
		this.id = id;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public void setErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
	}

	public void setTypeSlug(final String typeSlug) {
		this.typeSlug = typeSlug;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public void setPriority(final String priority) {
		this.priority = priority;
	}

	public void setReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
	}

	public void setAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
	}

	public void setApplicantName(final String applicantName) {
		this.applicantName = applicantName;
	}

	public void setProcessDefinitionName(final String processDefinitionName) {
		this.processDefinitionName = processDefinitionName;
	}

	public void setProcessInstanceId(final String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public void setTouched(final OffsetDateTime touched) {
		this.touched = touched;
	}

	public Errand withId(final String id) {
		this.id = id;
		return this;
	}

	public Errand withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public Errand withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public Errand withErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
		return this;
	}

	public Errand withTypeSlug(final String typeSlug) {
		this.typeSlug = typeSlug;
		return this;
	}

	public Errand withTitle(final String title) {
		this.title = title;
		return this;
	}

	public Errand withStatus(final String status) {
		this.status = status;
		return this;
	}

	public Errand withDescription(final String description) {
		this.description = description;
		return this;
	}

	public Errand withPriority(final String priority) {
		this.priority = priority;
		return this;
	}

	public Errand withReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
		return this;
	}

	public Errand withAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
		return this;
	}

	public Errand withApplicantName(final String applicantName) {
		this.applicantName = applicantName;
		return this;
	}

	public Errand withProcessDefinitionName(final String processDefinitionName) {
		this.processDefinitionName = processDefinitionName;
		return this;
	}

	public Errand withProcessInstanceId(final String processInstanceId) {
		this.processInstanceId = processInstanceId;
		return this;
	}

	public Errand withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public Errand withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public Errand withTouched(final OffsetDateTime touched) {
		this.touched = touched;
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
			&& Objects.equals(assignedUserId, errand.assignedUserId) && Objects.equals(applicantName, errand.applicantName)
			&& Objects.equals(processDefinitionName, errand.processDefinitionName)
			&& Objects.equals(processInstanceId, errand.processInstanceId)
			&& Objects.equals(created, errand.created) && Objects.equals(modified, errand.modified)
			&& Objects.equals(touched, errand.touched);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, errandNumber, typeSlug, title, status, description, priority,
			reporterUserId, assignedUserId, applicantName, processDefinitionName, processInstanceId, created, modified, touched);
	}

	@Override
	public String toString() {
		return "Errand{id='" + id + "', municipalityId='" + municipalityId + "', namespace='" + namespace
			+ "', errandNumber='" + errandNumber + "', typeSlug='" + typeSlug + "', status='" + status
			+ "', title='" + title + "', description='" + description + "', priority='" + priority
			+ "', reporterUserId='" + reporterUserId + "', assignedUserId='" + assignedUserId
			+ "', applicantName='" + applicantName
			+ "', processDefinitionName='" + processDefinitionName + "', processInstanceId='" + processInstanceId
			+ "', created=" + created + ", modified=" + modified + ", touched=" + touched + '}';
	}
}
