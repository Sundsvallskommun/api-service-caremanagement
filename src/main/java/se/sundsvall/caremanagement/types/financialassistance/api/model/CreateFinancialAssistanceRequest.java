package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import se.sundsvall.caremanagement.core.api.validation.groups.OnCreate;

@Schema(description = "Request to create a financial assistance errand.")
public class CreateFinancialAssistanceRequest {

	@Schema(description = "Title of the errand", examples = "Ansökan om ekonomiskt bistånd")
	@NotBlank(groups = OnCreate.class)
	private String title;

	@Schema(description = "Description of the errand", examples = "Återansökan om hyra för juni 2026")
	private String description;

	@Schema(description = "Priority of the errand", examples = "HIGH")
	private String priority;

	@Schema(description = "Id of the reporting user", examples = "joe01doe")
	private String reporterUserId;

	@Schema(description = "Id of the assigned user", examples = "jane02doe")
	private String assignedUserId;

	@Schema(description = "The typed financial assistance application payload")
	@Valid
	@NotNull(groups = OnCreate.class)
	private FinancialAssistanceData data;

	public static CreateFinancialAssistanceRequest create() {
		return new CreateFinancialAssistanceRequest();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public CreateFinancialAssistanceRequest withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public CreateFinancialAssistanceRequest withDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(final String priority) {
		this.priority = priority;
	}

	public CreateFinancialAssistanceRequest withPriority(final String priority) {
		this.priority = priority;
		return this;
	}

	public String getReporterUserId() {
		return reporterUserId;
	}

	public void setReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
	}

	public CreateFinancialAssistanceRequest withReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
		return this;
	}

	public String getAssignedUserId() {
		return assignedUserId;
	}

	public void setAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
	}

	public CreateFinancialAssistanceRequest withAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
		return this;
	}

	public FinancialAssistanceData getData() {
		return data;
	}

	public void setData(final FinancialAssistanceData data) {
		this.data = data;
	}

	public CreateFinancialAssistanceRequest withData(final FinancialAssistanceData data) {
		this.data = data;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final CreateFinancialAssistanceRequest that = (CreateFinancialAssistanceRequest) o;
		return Objects.equals(title, that.title) && Objects.equals(description, that.description)
			&& Objects.equals(priority, that.priority) && Objects.equals(reporterUserId, that.reporterUserId)
			&& Objects.equals(assignedUserId, that.assignedUserId) && Objects.equals(data, that.data);
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, description, priority, reporterUserId, assignedUserId, data);
	}

	@Override
	public String toString() {
		return "CreateFinancialAssistanceRequest{title='" + title + "', description='" + description + "', priority='"
			+ priority + "', reporterUserId='" + reporterUserId + "', assignedUserId='" + assignedUserId + "', data=" + data + '}';
	}
}
