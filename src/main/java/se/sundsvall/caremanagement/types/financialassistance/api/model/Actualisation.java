package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * A single Lifecare actualisation (case intake) registered on a person — the read model the frontend lists so a
 * caseworker can pick which actualisation to archive a supplementary application to. The personnummer
 * is deliberately not exposed.
 */
@Schema(description = "A Lifecare actualisation (case intake) registered on a person.")
public class Actualisation {

	@Schema(description = "The Lifecare actualisation id", examples = "5012")
	private Integer id;

	@Schema(description = "The actualisation type", examples = "Ansökan")
	private String type;

	@Schema(description = "The actualisation name", examples = "Ekonomiskt bistånd")
	private String name;

	@Schema(description = "The actualisation date as Lifecare reports it", examples = "2026-06-01")
	private String date;

	@Schema(description = "The reason for the actualisation", examples = "Nyansökan")
	private String reason;

	@Schema(description = "What the actualisation regards", examples = "Försörjningsstöd")
	private String regards;

	@Schema(description = "Who the actualisation came from", examples = "Den enskilde")
	private String fromWho;

	@Schema(description = "The caseworker the actualisation is registered on", examples = "Anna Andersson")
	private String caseworker;

	@Schema(description = "The organization the actualisation belongs to", examples = "IFO")
	private String organization;

	@Schema(description = "The actualisation status", examples = "Pågående")
	private String status;

	@Schema(description = "The linked investigation id, when any", examples = "8801")
	private Integer investigationId;

	@Schema(description = "The linked service id, when any", examples = "7700")
	private Integer serviceId;

	@Schema(description = "The linked decision id, when any", examples = "9900")
	private Integer decisionId;

	public static Actualisation create() {
		return new Actualisation();
	}

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public Actualisation withId(final Integer id) {
		this.id = id;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Actualisation withType(final String type) {
		this.type = type;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Actualisation withName(final String name) {
		this.name = name;
		return this;
	}

	public String getDate() {
		return date;
	}

	public void setDate(final String date) {
		this.date = date;
	}

	public Actualisation withDate(final String date) {
		this.date = date;
		return this;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(final String reason) {
		this.reason = reason;
	}

	public Actualisation withReason(final String reason) {
		this.reason = reason;
		return this;
	}

	public String getRegards() {
		return regards;
	}

	public void setRegards(final String regards) {
		this.regards = regards;
	}

	public Actualisation withRegards(final String regards) {
		this.regards = regards;
		return this;
	}

	public String getFromWho() {
		return fromWho;
	}

	public void setFromWho(final String fromWho) {
		this.fromWho = fromWho;
	}

	public Actualisation withFromWho(final String fromWho) {
		this.fromWho = fromWho;
		return this;
	}

	public String getCaseworker() {
		return caseworker;
	}

	public void setCaseworker(final String caseworker) {
		this.caseworker = caseworker;
	}

	public Actualisation withCaseworker(final String caseworker) {
		this.caseworker = caseworker;
		return this;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(final String organization) {
		this.organization = organization;
	}

	public Actualisation withOrganization(final String organization) {
		this.organization = organization;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public Actualisation withStatus(final String status) {
		this.status = status;
		return this;
	}

	public Integer getInvestigationId() {
		return investigationId;
	}

	public void setInvestigationId(final Integer investigationId) {
		this.investigationId = investigationId;
	}

	public Actualisation withInvestigationId(final Integer investigationId) {
		this.investigationId = investigationId;
		return this;
	}

	public Integer getServiceId() {
		return serviceId;
	}

	public void setServiceId(final Integer serviceId) {
		this.serviceId = serviceId;
	}

	public Actualisation withServiceId(final Integer serviceId) {
		this.serviceId = serviceId;
		return this;
	}

	public Integer getDecisionId() {
		return decisionId;
	}

	public void setDecisionId(final Integer decisionId) {
		this.decisionId = decisionId;
	}

	public Actualisation withDecisionId(final Integer decisionId) {
		this.decisionId = decisionId;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final Actualisation that = (Actualisation) o;
		return Objects.equals(id, that.id) && Objects.equals(type, that.type) && Objects.equals(name, that.name) && Objects.equals(date, that.date)
			&& Objects.equals(reason, that.reason) && Objects.equals(regards, that.regards) && Objects.equals(fromWho, that.fromWho)
			&& Objects.equals(caseworker, that.caseworker) && Objects.equals(organization, that.organization) && Objects.equals(status, that.status)
			&& Objects.equals(investigationId, that.investigationId) && Objects.equals(serviceId, that.serviceId) && Objects.equals(decisionId, that.decisionId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, type, name, date, reason, regards, fromWho, caseworker, organization, status, investigationId, serviceId, decisionId);
	}

	@Override
	public String toString() {
		return "Actualisation{id=" + id + ", type='" + type + "', name='" + name + "', date='" + date + "', reason='" + reason + "', regards='" + regards
			+ "', fromWho='" + fromWho + "', caseworker='" + caseworker + "', organization='" + organization + "', status='" + status
			+ "', investigationId=" + investigationId + ", serviceId=" + serviceId + ", decisionId=" + decisionId + "}";
	}
}
