package se.sundsvall.caremanagement.core.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * PATCH semantics: a null field means "leave the entity value untouched". Subcollections
 * (attachments / stakeholders / decisions) have their own subresources and are not patched
 * through here. The type-slug is immutable post-create — switch via delete + recreate.
 */
@Schema(description = "PatchErrand model — patchable envelope fields only")
public class PatchErrand {

	@Schema(description = "Title for the errand", examples = "Title of the errand")
	private String title;

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

	public static PatchErrand create() {
		return new PatchErrand();
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

	public PatchErrand withTitle(final String v) {
		this.title = v;
		return this;
	}

	public PatchErrand withStatus(final String v) {
		this.status = v;
		return this;
	}

	public PatchErrand withDescription(final String v) {
		this.description = v;
		return this;
	}

	public PatchErrand withPriority(final String v) {
		this.priority = v;
		return this;
	}

	public PatchErrand withReporterUserId(final String v) {
		this.reporterUserId = v;
		return this;
	}

	public PatchErrand withAssignedUserId(final String v) {
		this.assignedUserId = v;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final PatchErrand that = (PatchErrand) o;
		return Objects.equals(title, that.title) && Objects.equals(status, that.status)
			&& Objects.equals(description, that.description) && Objects.equals(priority, that.priority)
			&& Objects.equals(reporterUserId, that.reporterUserId) && Objects.equals(assignedUserId, that.assignedUserId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, status, description, priority, reporterUserId, assignedUserId);
	}

	@Override
	public String toString() {
		return "PatchErrand{" +
			"title='" + title + '\'' +
			", status='" + status + '\'' +
			", description='" + description + '\'' +
			", priority='" + priority + '\'' +
			", reporterUserId='" + reporterUserId + '\'' +
			", assignedUserId='" + assignedUserId + '\'' +
			'}';
	}
}
