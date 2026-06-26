package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * A Lifecare beslut with the full breakdown — the decision metadata and the persons it covers — the read model the
 * frontend renders so a caseworker can review a registered decision.
 */
@Schema(description = "A Lifecare beslut, full breakdown.")
public class LifecareDecision {

	@Schema(description = "The Lifecare decision id", examples = "9900")
	private Integer id;

	@Schema(description = "The decision date as Lifecare reports it", examples = "2026-06-02")
	private String date;

	@Schema(description = "The decision type", examples = "Bifall")
	private String type;

	@Schema(description = "The start date of the decision period", examples = "2026-06-01")
	private String fromDate;

	@Schema(description = "The end date of the decision period", examples = "2026-06-30")
	private String toDate;

	@Schema(description = "The reason for the decision", examples = "Beviljas enligt norm")
	private String reason;

	@Schema(description = "The decision maker", examples = "Anna Andersson")
	private String decisionMaker;

	@Schema(description = "The organization the decision belongs to", examples = "IFO")
	private String organization;

	@Schema(description = "The decided amount", examples = "8500.0")
	private Double amount;

	@Schema(description = "The co-applicant the decision covers, when any", examples = "198001019999")
	private String coApplicant;

	@Schema(description = "The reason concerning the co-applicant, when any", examples = "Sammanboende")
	private String reasonCoApplicant;

	@ArraySchema(schema = @Schema(implementation = LifecareDecisionPerson.class))
	private List<LifecareDecisionPerson> persons;

	public static LifecareDecision create() {
		return new LifecareDecision();
	}

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public LifecareDecision withId(final Integer id) {
		this.id = id;
		return this;
	}

	public String getDate() {
		return date;
	}

	public void setDate(final String date) {
		this.date = date;
	}

	public LifecareDecision withDate(final String date) {
		this.date = date;
		return this;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public LifecareDecision withType(final String type) {
		this.type = type;
		return this;
	}

	public String getFromDate() {
		return fromDate;
	}

	public void setFromDate(final String fromDate) {
		this.fromDate = fromDate;
	}

	public LifecareDecision withFromDate(final String fromDate) {
		this.fromDate = fromDate;
		return this;
	}

	public String getToDate() {
		return toDate;
	}

	public void setToDate(final String toDate) {
		this.toDate = toDate;
	}

	public LifecareDecision withToDate(final String toDate) {
		this.toDate = toDate;
		return this;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(final String reason) {
		this.reason = reason;
	}

	public LifecareDecision withReason(final String reason) {
		this.reason = reason;
		return this;
	}

	public String getDecisionMaker() {
		return decisionMaker;
	}

	public void setDecisionMaker(final String decisionMaker) {
		this.decisionMaker = decisionMaker;
	}

	public LifecareDecision withDecisionMaker(final String decisionMaker) {
		this.decisionMaker = decisionMaker;
		return this;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(final String organization) {
		this.organization = organization;
	}

	public LifecareDecision withOrganization(final String organization) {
		this.organization = organization;
		return this;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(final Double amount) {
		this.amount = amount;
	}

	public LifecareDecision withAmount(final Double amount) {
		this.amount = amount;
		return this;
	}

	public String getCoApplicant() {
		return coApplicant;
	}

	public void setCoApplicant(final String coApplicant) {
		this.coApplicant = coApplicant;
	}

	public LifecareDecision withCoApplicant(final String coApplicant) {
		this.coApplicant = coApplicant;
		return this;
	}

	public String getReasonCoApplicant() {
		return reasonCoApplicant;
	}

	public void setReasonCoApplicant(final String reasonCoApplicant) {
		this.reasonCoApplicant = reasonCoApplicant;
	}

	public LifecareDecision withReasonCoApplicant(final String reasonCoApplicant) {
		this.reasonCoApplicant = reasonCoApplicant;
		return this;
	}

	public List<LifecareDecisionPerson> getPersons() {
		return persons;
	}

	public void setPersons(final List<LifecareDecisionPerson> persons) {
		this.persons = persons;
	}

	public LifecareDecision withPersons(final List<LifecareDecisionPerson> persons) {
		this.persons = persons;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final LifecareDecision that = (LifecareDecision) o;
		return Objects.equals(id, that.id) && Objects.equals(date, that.date) && Objects.equals(type, that.type) && Objects.equals(fromDate, that.fromDate)
			&& Objects.equals(toDate, that.toDate) && Objects.equals(reason, that.reason) && Objects.equals(decisionMaker, that.decisionMaker)
			&& Objects.equals(organization, that.organization) && Objects.equals(amount, that.amount) && Objects.equals(coApplicant, that.coApplicant)
			&& Objects.equals(reasonCoApplicant, that.reasonCoApplicant) && Objects.equals(persons, that.persons);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, date, type, fromDate, toDate, reason, decisionMaker, organization, amount, coApplicant, reasonCoApplicant, persons);
	}

	@Override
	public String toString() {
		return "LifecareDecision{id=" + id + ", date='" + date + "', type='" + type + "', fromDate='" + fromDate + "', toDate='" + toDate + "', reason='" + reason
			+ "', decisionMaker='" + decisionMaker + "', organization='" + organization + "', amount=" + amount + ", coApplicant='" + coApplicant
			+ "', reasonCoApplicant='" + reasonCoApplicant + "', persons=" + persons + "}";
	}
}
