package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A bevakning on the errand's insats in Lifecare, as the sidebar shows it. The personnummer on the Lifecare row is left out.")
public class LifecareReminder {

	@Schema(description = "The Lifecare reminder id", examples = "40")
	private Integer id;

	@Schema(description = "Bevakningsdatum, YYYY-MM-DD", examples = "2026-09-30")
	private String date;

	@Schema(description = "The status text", examples = "Ej påbörjad")
	private String status;

	@Schema(description = "Lifecare's status code", examples = "3")
	private Integer statusCode;

	@Schema(description = "The priority text", examples = "Normal")
	private String priority;

	@Schema(description = "Lifecare's priority code", examples = "2")
	private Integer priorityCode;

	@Schema(description = "The kind of bevakning", examples = "Manuell bevakning insats")
	private String type;

	@Schema(description = "What the bevakning hangs on", examples = "IFO.Insats")
	private String objectType;

	@Schema(description = "The bevakning text", examples = "Kontrollera hyran")
	private String text;

	@Schema(description = "The caseworker it is bevakad av, by name (the id when Lifecare gives no name)", examples = "Test Handläggare")
	private String caseworker;

	@Schema(description = "The caseworker's Lifecare id", examples = "TEST")
	private String caseworkerId;

	public static LifecareReminder create() {
		return new LifecareReminder();
	}

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public LifecareReminder withId(final Integer id) {
		this.id = id;
		return this;
	}

	public String getDate() {
		return date;
	}

	public void setDate(final String date) {
		this.date = date;
	}

	public LifecareReminder withDate(final String date) {
		this.date = date;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public LifecareReminder withStatus(final String status) {
		this.status = status;
		return this;
	}

	public Integer getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(final Integer statusCode) {
		this.statusCode = statusCode;
	}

	public LifecareReminder withStatusCode(final Integer statusCode) {
		this.statusCode = statusCode;
		return this;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(final String priority) {
		this.priority = priority;
	}

	public LifecareReminder withPriority(final String priority) {
		this.priority = priority;
		return this;
	}

	public Integer getPriorityCode() {
		return priorityCode;
	}

	public void setPriorityCode(final Integer priorityCode) {
		this.priorityCode = priorityCode;
	}

	public LifecareReminder withPriorityCode(final Integer priorityCode) {
		this.priorityCode = priorityCode;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public LifecareReminder withType(final String type) {
		this.type = type;
		return this;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(final String objectType) {
		this.objectType = objectType;
	}

	public LifecareReminder withObjectType(final String objectType) {
		this.objectType = objectType;
		return this;
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	public LifecareReminder withText(final String text) {
		this.text = text;
		return this;
	}

	public String getCaseworker() {
		return caseworker;
	}

	public void setCaseworker(final String caseworker) {
		this.caseworker = caseworker;
	}

	public LifecareReminder withCaseworker(final String caseworker) {
		this.caseworker = caseworker;
		return this;
	}

	public String getCaseworkerId() {
		return caseworkerId;
	}

	public void setCaseworkerId(final String caseworkerId) {
		this.caseworkerId = caseworkerId;
	}

	public LifecareReminder withCaseworkerId(final String caseworkerId) {
		this.caseworkerId = caseworkerId;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final LifecareReminder that = (LifecareReminder) o;
		return Objects.equals(id, that.id) && Objects.equals(date, that.date) && Objects.equals(status, that.status) && Objects.equals(statusCode, that.statusCode) && Objects.equals(priority, that.priority) && Objects.equals(priorityCode,
			that.priorityCode) && Objects.equals(type, that.type) && Objects.equals(objectType, that.objectType) && Objects.equals(text, that.text) && Objects.equals(caseworker, that.caseworker) && Objects.equals(caseworkerId, that.caseworkerId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, date, status, statusCode, priority, priorityCode, type, objectType, text, caseworker, caseworkerId);
	}

	@Override
	public String toString() {
		return "LifecareReminder{" + "id=" + id + ", date='" + date + '\'' + ", status='" + status + '\'' + ", statusCode=" + statusCode + ", priority='" + priority + '\'' + ", priorityCode=" + priorityCode + ", type='" + type + '\'' + ", objectType='"
			+ objectType + '\'' + ", text='" + text + '\'' + ", caseworker='" + caseworker + '\'' + ", caseworkerId='" + caseworkerId + '\'' + '}';
	}
}
