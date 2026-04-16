package se.sundsvall.caremanagement.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import se.sundsvall.caremanagement.api.validation.groups.OnCreate;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Schema(description = "Errand model")
public class Errand {

	@Schema(description = "Unique identifier of the errand", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	@Null(groups = OnCreate.class)
	private String id;

	@Schema(description = "Municipality id", examples = "2281", accessMode = READ_ONLY)
	private String municipalityId;

	@Schema(description = "Namespace", examples = "MY_NAMESPACE", accessMode = READ_ONLY)
	private String namespace;

	@Schema(description = "Title for the errand", examples = "Title of the errand")
	@NotBlank(groups = OnCreate.class)
	private String title;

	@Schema(description = "Category for the errand", examples = "CATEGORY-1")
	private String category;

	@Schema(description = "Type for the errand", examples = "TYPE-1")
	private String type;

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

	@Schema(description = "Contact reason (name of a CONTACT_REASON lookup)", examples = "PHONE")
	private String contactReason;

	@Schema(description = "Contact reason description", examples = "The reporter called in")
	private String contactReasonDescription;

	@Schema(description = "External tags associated with the errand")
	@Valid
	private List<ExternalTag> externalTags;

	@Schema(description = "Stakeholders associated with the errand")
	@Valid
	private List<Stakeholder> stakeholders;

	@Schema(description = "Parameters for the errand")
	@Valid
	private List<Parameter> parameters;

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

	public void setId(final String id) {
		this.id = id;
	}

	public Errand withId(final String id) {
		this.id = id;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public Errand withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public Errand withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public Errand withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	public Errand withCategory(final String category) {
		this.category = category;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Errand withType(final String type) {
		this.type = type;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public Errand withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public Errand withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(final String priority) {
		this.priority = priority;
	}

	public Errand withPriority(final String priority) {
		this.priority = priority;
		return this;
	}

	public String getReporterUserId() {
		return reporterUserId;
	}

	public void setReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
	}

	public Errand withReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
		return this;
	}

	public String getAssignedUserId() {
		return assignedUserId;
	}

	public void setAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
	}

	public Errand withAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
		return this;
	}

	public String getContactReason() {
		return contactReason;
	}

	public void setContactReason(final String contactReason) {
		this.contactReason = contactReason;
	}

	public Errand withContactReason(final String contactReason) {
		this.contactReason = contactReason;
		return this;
	}

	public String getContactReasonDescription() {
		return contactReasonDescription;
	}

	public void setContactReasonDescription(final String contactReasonDescription) {
		this.contactReasonDescription = contactReasonDescription;
	}

	public Errand withContactReasonDescription(final String contactReasonDescription) {
		this.contactReasonDescription = contactReasonDescription;
		return this;
	}

	public List<ExternalTag> getExternalTags() {
		return externalTags;
	}

	public void setExternalTags(final List<ExternalTag> externalTags) {
		this.externalTags = externalTags;
	}

	public Errand withExternalTags(final List<ExternalTag> externalTags) {
		this.externalTags = externalTags;
		return this;
	}

	public List<Stakeholder> getStakeholders() {
		return stakeholders;
	}

	public void setStakeholders(final List<Stakeholder> stakeholders) {
		this.stakeholders = stakeholders;
	}

	public Errand withStakeholders(final List<Stakeholder> stakeholders) {
		this.stakeholders = stakeholders;
		return this;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(final List<Parameter> parameters) {
		this.parameters = parameters;
	}

	public Errand withParameters(final List<Parameter> parameters) {
		this.parameters = parameters;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public Errand withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public Errand withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public OffsetDateTime getTouched() {
		return touched;
	}

	public void setTouched(final OffsetDateTime touched) {
		this.touched = touched;
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
		return Objects.equals(id, errand.id) && Objects.equals(municipalityId, errand.municipalityId) && Objects.equals(namespace, errand.namespace) && Objects.equals(title, errand.title) && Objects.equals(
			category, errand.category) && Objects.equals(type, errand.type) && Objects.equals(status, errand.status) && Objects.equals(description, errand.description) && Objects.equals(priority, errand.priority)
			&& Objects.equals(reporterUserId, errand.reporterUserId) && Objects.equals(assignedUserId, errand.assignedUserId) && Objects.equals(contactReason, errand.contactReason) && Objects.equals(
				contactReasonDescription, errand.contactReasonDescription) && Objects.equals(externalTags, errand.externalTags) && Objects.equals(stakeholders, errand.stakeholders) && Objects.equals(parameters, errand.parameters)
			&& Objects.equals(created, errand.created) && Objects.equals(modified, errand.modified) && Objects.equals(touched, errand.touched);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, municipalityId, namespace, title, category, type, status, description, priority, reporterUserId, assignedUserId, contactReason, contactReasonDescription, externalTags, stakeholders, parameters, created, modified, touched);
	}

	@Override
	public String toString() {
		return "Errand{" +
			"id='" + id + '\'' +
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
			", contactReason='" + contactReason + '\'' +
			", contactReasonDescription='" + contactReasonDescription + '\'' +
			", externalTags=" + externalTags +
			", stakeholders=" + stakeholders +
			", parameters=" + parameters +
			", created=" + created +
			", modified=" + modified +
			", touched=" + touched +
			'}';
	}
}
