package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * An income row of a previous Lifecare beräkning.
 */
@Schema(description = "An income row of a previous Lifecare beräkning.")
public class NormberakningPreviousIncome {

	@Schema(description = "The income type", examples = "Aktivitetsstöd")
	private String type;

	@Schema(description = "The applicant amount")
	private BigDecimal amountApplicant;

	@Schema(description = "The date the applicant amount was looked up")
	private String applicantSearchDate;

	@Schema(description = "The co-applicant amount")
	private BigDecimal amountCoApplicant;

	@Schema(description = "The date the co-applicant amount was looked up")
	private String coApplicantSearchDate;

	public static NormberakningPreviousIncome create() {
		return new NormberakningPreviousIncome();
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public NormberakningPreviousIncome withType(final String type) {
		this.type = type;
		return this;
	}

	public BigDecimal getAmountApplicant() {
		return amountApplicant;
	}

	public void setAmountApplicant(final BigDecimal amountApplicant) {
		this.amountApplicant = amountApplicant;
	}

	public NormberakningPreviousIncome withAmountApplicant(final BigDecimal amountApplicant) {
		this.amountApplicant = amountApplicant;
		return this;
	}

	public String getApplicantSearchDate() {
		return applicantSearchDate;
	}

	public void setApplicantSearchDate(final String applicantSearchDate) {
		this.applicantSearchDate = applicantSearchDate;
	}

	public NormberakningPreviousIncome withApplicantSearchDate(final String applicantSearchDate) {
		this.applicantSearchDate = applicantSearchDate;
		return this;
	}

	public BigDecimal getAmountCoApplicant() {
		return amountCoApplicant;
	}

	public void setAmountCoApplicant(final BigDecimal amountCoApplicant) {
		this.amountCoApplicant = amountCoApplicant;
	}

	public NormberakningPreviousIncome withAmountCoApplicant(final BigDecimal amountCoApplicant) {
		this.amountCoApplicant = amountCoApplicant;
		return this;
	}

	public String getCoApplicantSearchDate() {
		return coApplicantSearchDate;
	}

	public void setCoApplicantSearchDate(final String coApplicantSearchDate) {
		this.coApplicantSearchDate = coApplicantSearchDate;
	}

	public NormberakningPreviousIncome withCoApplicantSearchDate(final String coApplicantSearchDate) {
		this.coApplicantSearchDate = coApplicantSearchDate;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final NormberakningPreviousIncome that = (NormberakningPreviousIncome) o;
		return Objects.equals(type, that.type) && Objects.equals(amountApplicant, that.amountApplicant) && Objects.equals(applicantSearchDate, that.applicantSearchDate) && Objects.equals(amountCoApplicant, that.amountCoApplicant) && Objects.equals(
			coApplicantSearchDate, that.coApplicantSearchDate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, amountApplicant, applicantSearchDate, amountCoApplicant, coApplicantSearchDate);
	}

	@Override
	public String toString() {
		return "NormberakningPreviousIncome{" +
			"type='" + type + '\'' +
			", amountApplicant=" + amountApplicant +
			", applicantSearchDate='" + applicantSearchDate + '\'' +
			", amountCoApplicant=" + amountCoApplicant +
			", coApplicantSearchDate='" + coApplicantSearchDate + '\'' +
			'}';
	}
}
