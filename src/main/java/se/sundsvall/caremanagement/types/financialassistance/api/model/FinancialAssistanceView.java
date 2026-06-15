package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.Objects;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(description = "A financial assistance errand with its typed application payload.")
public class FinancialAssistanceView {

	@Schema(description = "Unique id", examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "Errand number", examples = "EB-2026-00042", accessMode = READ_ONLY)
	private String errandNumber;

	@Schema(description = "Municipality id", examples = "2281", accessMode = READ_ONLY)
	private String municipalityId;

	@Schema(description = "Namespace", examples = "FINANCIAL_ASSISTANCE", accessMode = READ_ONLY)
	private String namespace;

	@Schema(description = "Type slug", examples = "financial-assistance", accessMode = READ_ONLY)
	private String typeSlug;

	@Schema(description = "Title of the errand", examples = "Ansökan om ekonomiskt bistånd")
	private String title;

	@Schema(description = "Status of the errand", examples = "ONGOING")
	private String status;

	@Schema(description = "Priority of the errand", examples = "HIGH")
	private String priority;

	@Schema(description = "Id of the reporting user", examples = "joe01doe")
	private String reporterUserId;

	@Schema(description = "Id of the assigned user", examples = "jane02doe")
	private String assignedUserId;

	@Schema(description = "Id of the started process instance", examples = "8d2e1c3a-4f56-7890-abcd-ef1234567890", accessMode = READ_ONLY)
	private String processInstanceId;

	@Schema(description = "Created", accessMode = READ_ONLY)
	private OffsetDateTime created;

	@Schema(description = "Modified", accessMode = READ_ONLY)
	private OffsetDateTime modified;

	@Schema(description = "Touched", accessMode = READ_ONLY)
	private OffsetDateTime touched;

	@Schema(description = "The typed financial assistance application payload")
	private FinancialAssistanceData data;

	public static FinancialAssistanceView create() {
		return new FinancialAssistanceView();
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public FinancialAssistanceView withId(final String id) {
		this.id = id;
		return this;
	}

	public String getErrandNumber() {
		return errandNumber;
	}

	public void setErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
	}

	public FinancialAssistanceView withErrandNumber(final String errandNumber) {
		this.errandNumber = errandNumber;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public FinancialAssistanceView withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(final String namespace) {
		this.namespace = namespace;
	}

	public FinancialAssistanceView withNamespace(final String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getTypeSlug() {
		return typeSlug;
	}

	public void setTypeSlug(final String typeSlug) {
		this.typeSlug = typeSlug;
	}

	public FinancialAssistanceView withTypeSlug(final String typeSlug) {
		this.typeSlug = typeSlug;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public FinancialAssistanceView withTitle(final String title) {
		this.title = title;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public FinancialAssistanceView withStatus(final String status) {
		this.status = status;
		return this;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(final String priority) {
		this.priority = priority;
	}

	public FinancialAssistanceView withPriority(final String priority) {
		this.priority = priority;
		return this;
	}

	public String getReporterUserId() {
		return reporterUserId;
	}

	public void setReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
	}

	public FinancialAssistanceView withReporterUserId(final String reporterUserId) {
		this.reporterUserId = reporterUserId;
		return this;
	}

	public String getAssignedUserId() {
		return assignedUserId;
	}

	public void setAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
	}

	public FinancialAssistanceView withAssignedUserId(final String assignedUserId) {
		this.assignedUserId = assignedUserId;
		return this;
	}

	public String getProcessInstanceId() {
		return processInstanceId;
	}

	public void setProcessInstanceId(final String processInstanceId) {
		this.processInstanceId = processInstanceId;
	}

	public FinancialAssistanceView withProcessInstanceId(final String processInstanceId) {
		this.processInstanceId = processInstanceId;
		return this;
	}

	public OffsetDateTime getCreated() {
		return created;
	}

	public void setCreated(final OffsetDateTime created) {
		this.created = created;
	}

	public FinancialAssistanceView withCreated(final OffsetDateTime created) {
		this.created = created;
		return this;
	}

	public OffsetDateTime getModified() {
		return modified;
	}

	public void setModified(final OffsetDateTime modified) {
		this.modified = modified;
	}

	public FinancialAssistanceView withModified(final OffsetDateTime modified) {
		this.modified = modified;
		return this;
	}

	public OffsetDateTime getTouched() {
		return touched;
	}

	public void setTouched(final OffsetDateTime touched) {
		this.touched = touched;
	}

	public FinancialAssistanceView withTouched(final OffsetDateTime touched) {
		this.touched = touched;
		return this;
	}

	public FinancialAssistanceData getData() {
		return data;
	}

	public void setData(final FinancialAssistanceData data) {
		this.data = data;
	}

	public FinancialAssistanceView withData(final FinancialAssistanceData data) {
		this.data = data;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FinancialAssistanceView that = (FinancialAssistanceView) o;
		return Objects.equals(id, that.id) && Objects.equals(errandNumber, that.errandNumber)
			&& Objects.equals(municipalityId, that.municipalityId) && Objects.equals(namespace, that.namespace)
			&& Objects.equals(typeSlug, that.typeSlug) && Objects.equals(title, that.title)
			&& Objects.equals(status, that.status) && Objects.equals(priority, that.priority)
			&& Objects.equals(reporterUserId, that.reporterUserId) && Objects.equals(assignedUserId, that.assignedUserId)
			&& Objects.equals(processInstanceId, that.processInstanceId) && Objects.equals(created, that.created)
			&& Objects.equals(modified, that.modified) && Objects.equals(touched, that.touched) && Objects.equals(data, that.data);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, errandNumber, municipalityId, namespace, typeSlug, title, status, priority, reporterUserId,
			assignedUserId, processInstanceId, created, modified, touched, data);
	}

	@Override
	public String toString() {
		return "FinancialAssistanceView{id='" + id + "', errandNumber='" + errandNumber + "', municipalityId='"
			+ municipalityId + "', namespace='" + namespace + "', typeSlug='" + typeSlug + "', title='" + title
			+ "', status='" + status + "', priority='" + priority + "', reporterUserId='" + reporterUserId
			+ "', assignedUserId='" + assignedUserId + "', processInstanceId='" + processInstanceId + "', created=" + created
			+ ", modified=" + modified + ", touched=" + touched + ", data=" + data + '}';
	}
}
