package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * A single income row on a Lifecare calculation, split between the applicant and the co-applicant with the search
 * date Lifecare used for each.
 */
@Schema(description = "An income row on a Lifecare calculation.")
public class LifecareCalculationIncome {

	@Schema(description = "The income type", examples = "Lön")
	private String type;

	@Schema(description = "The income amount for the applicant", examples = "12000.0")
	private Double amountApplicant;

	@Schema(description = "The search date Lifecare used for the applicant", examples = "2026-05-15")
	private String applicantSearchDate;

	@Schema(description = "The income amount for the co-applicant", examples = "0.0")
	private Double amountCoApplicant;

	@Schema(description = "The search date Lifecare used for the co-applicant", examples = "2026-05-15")
	private String coApplicantSearchDate;

	public static LifecareCalculationIncome create() {
		return new LifecareCalculationIncome();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public LifecareCalculationIncome withType(final String type) {
		this.type = type;
		return this;
	}

	public Double getAmountApplicant() {
		return amountApplicant;
	}

	public void setAmountApplicant(final Double amountApplicant) {
		this.amountApplicant = amountApplicant;
	}

	public LifecareCalculationIncome withAmountApplicant(final Double amountApplicant) {
		this.amountApplicant = amountApplicant;
		return this;
	}

	public String getApplicantSearchDate() {
		return applicantSearchDate;
	}

	public void setApplicantSearchDate(final String applicantSearchDate) {
		this.applicantSearchDate = applicantSearchDate;
	}

	public LifecareCalculationIncome withApplicantSearchDate(final String applicantSearchDate) {
		this.applicantSearchDate = applicantSearchDate;
		return this;
	}

	public Double getAmountCoApplicant() {
		return amountCoApplicant;
	}

	public void setAmountCoApplicant(final Double amountCoApplicant) {
		this.amountCoApplicant = amountCoApplicant;
	}

	public LifecareCalculationIncome withAmountCoApplicant(final Double amountCoApplicant) {
		this.amountCoApplicant = amountCoApplicant;
		return this;
	}

	public String getCoApplicantSearchDate() {
		return coApplicantSearchDate;
	}

	public void setCoApplicantSearchDate(final String coApplicantSearchDate) {
		this.coApplicantSearchDate = coApplicantSearchDate;
	}

	public LifecareCalculationIncome withCoApplicantSearchDate(final String coApplicantSearchDate) {
		this.coApplicantSearchDate = coApplicantSearchDate;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final LifecareCalculationIncome that = (LifecareCalculationIncome) o;
		return Objects.equals(type, that.type) && Objects.equals(amountApplicant, that.amountApplicant)
			&& Objects.equals(applicantSearchDate, that.applicantSearchDate) && Objects.equals(amountCoApplicant, that.amountCoApplicant)
			&& Objects.equals(coApplicantSearchDate, that.coApplicantSearchDate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, amountApplicant, applicantSearchDate, amountCoApplicant, coApplicantSearchDate);
	}

	@Override
	public String toString() {
		return "LifecareCalculationIncome{type='" + type + "', amountApplicant=" + amountApplicant + ", applicantSearchDate='" + applicantSearchDate
			+ "', amountCoApplicant=" + amountCoApplicant + ", coApplicantSearchDate='" + coApplicantSearchDate + "'}";
	}
}
