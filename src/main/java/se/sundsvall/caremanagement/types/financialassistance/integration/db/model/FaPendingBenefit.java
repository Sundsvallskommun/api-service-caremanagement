package se.sundsvall.caremanagement.types.financialassistance.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class FaPendingBenefit {

	@Column(name = "benefit_name")
	private String benefitName;

	@Column(name = "applicant_name")
	private String applicantName;

	public static FaPendingBenefit create() {
		return new FaPendingBenefit();
	}

	public String getBenefitName() {
		return benefitName;
	}

	public String getApplicantName() {
		return applicantName;
	}

	public void setBenefitName(final String benefitName) {
		this.benefitName = benefitName;
	}

	public void setApplicantName(final String applicantName) {
		this.applicantName = applicantName;
	}

	public FaPendingBenefit withBenefitName(final String benefitName) {
		this.benefitName = benefitName;
		return this;
	}

	public FaPendingBenefit withApplicantName(final String applicantName) {
		this.applicantName = applicantName;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final FaPendingBenefit that = (FaPendingBenefit) o;
		return Objects.equals(benefitName, that.benefitName) && Objects.equals(applicantName, that.applicantName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(benefitName, applicantName);
	}

	@Override
	public String toString() {
		return "FaPendingBenefit{benefitName='" + benefitName + "', applicantName='" + applicantName + "'}";
	}
}
