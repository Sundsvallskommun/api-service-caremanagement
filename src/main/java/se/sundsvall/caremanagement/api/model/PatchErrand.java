package se.sundsvall.caremanagement.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;

@Schema(description = "PatchErrand model - only patchable fields")
public class PatchErrand {

	@Schema(description = "Title for the errand", examples = "Title of the errand")
	private String title;

	@Schema(description = "Category for the errand", examples = "CATEGORY-1")
	private String category;

	@Schema(description = "Type for the errand", examples = "TYPE-1")
	private String type;

	@Schema(description = "Status of the errand", examples = "NEW")
	private String status;

	@Schema(description = "Description of the errand")
	private String description;

	@Schema(description = "Priority of the errand", examples = "HIGH")
	private String priority;

	@Schema(description = "User id of the reporter", examples = "joe01doe")
	private String reporterUserId;

	@Schema(description = "User id of the assignee", examples = "jane02doe")
	private String assignedUserId;

	@Schema(description = "Contact reason (name of a CONTACT_REASON lookup)", examples = "PHONE")
	private String contactReason;

	@Schema(description = "Contact reason description")
	private String contactReasonDescription;

	@Schema(description = "External tags associated with the errand")
	@Valid
	private List<ExternalTag> externalTags;

	public static PatchErrand create() {
		return new PatchErrand();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public PatchErrand withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	public PatchErrand withCategory(final String category) {
		this.category = category;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public PatchErrand withType(final String type) {
		this.type = type;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public PatchErrand withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public PatchErrand withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(final String priority) {
		this.priority = priority;
	}

	public PatchErrand withPriority(final String priority) {
		this.priority = priority;
		return this;
	}

	public String getReporterUserId() {
		return reporterUserId;
	}

	public void setReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
	}

	public PatchErrand withReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
		return this;
	}

	public String getAssignedUserId() {
		return assignedUserId;
	}

	public void setAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
	}

	public PatchErrand withAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
		return this;
	}

	public String getContactReason() {
		return contactReason;
	}

	public void setContactReason(final String contactReason) {
		this.contactReason = contactReason;
	}

	public PatchErrand withContactReason(final String contactReason) {
		this.contactReason = contactReason;
		return this;
	}

	public String getContactReasonDescription() {
		return contactReasonDescription;
	}

	public void setContactReasonDescription(final String contactReasonDescription) {
		this.contactReasonDescription = contactReasonDescription;
	}

	public PatchErrand withContactReasonDescription(final String contactReasonDescription) {
		this.contactReasonDescription = contactReasonDescription;
		return this;
	}

	public List<ExternalTag> getExternalTags() {
		return externalTags;
	}

	public void setExternalTags(final List<ExternalTag> externalTags) {
		this.externalTags = externalTags;
	}

	public PatchErrand withExternalTags(final List<ExternalTag> externalTags) {
		this.externalTags = externalTags;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PatchErrand that = (PatchErrand) o;
		return Objects.equals(title, that.title) && Objects.equals(category, that.category) && Objects.equals(type, that.type) && Objects.equals(status, that.status)
			&& Objects.equals(description, that.description) && Objects.equals(priority, that.priority) && Objects.equals(reporterUserId, that.reporterUserId)
			&& Objects.equals(assignedUserId, that.assignedUserId) && Objects.equals(contactReason, that.contactReason) && Objects.equals(contactReasonDescription, that.contactReasonDescription)
			&& Objects.equals(externalTags, that.externalTags);
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, category, type, status, description, priority, reporterUserId, assignedUserId, contactReason, contactReasonDescription, externalTags);
	}

	@Override
	public String toString() {
		return "PatchErrand{" +
			"title='" + title + '\'' +
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
			'}';
	}
}
