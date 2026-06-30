package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "A benefit the applicant has applied for but not yet received a decision on.")
public class PendingBenefit {

	@Schema(description = "Name of the pending benefit", examples = "Bostadsbidrag")
	private String benefitName;

	@Schema(description = "Name of the person who applied for the benefit", examples = "Anna Andersson")
	private String applicantName;

	public static PendingBenefit create() {
		return new PendingBenefit();
	}

	public String getBenefitName() {
		return benefitName;
	}

	public void setBenefitName(final String benefitName) {
		this.benefitName = benefitName;
	}

	public PendingBenefit withBenefitName(final String benefitName) {
		this.benefitName = benefitName;
		return this;
	}

	public String getApplicantName() {
		return applicantName;
	}

	public void setApplicantName(final String applicantName) {
		this.applicantName = applicantName;
	}

	public PendingBenefit withApplicantName(final String applicantName) {
		this.applicantName = applicantName;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PendingBenefit that = (PendingBenefit) o;
		return Objects.equals(benefitName, that.benefitName) && Objects.equals(applicantName, that.applicantName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(benefitName, applicantName);
	}

	@Override
	public String toString() {
		return "PendingBenefit{benefitName='" + benefitName + "', applicantName='" + applicantName + "'}";
	}
}
