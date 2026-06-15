package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidPersonalNumber;

/**
 * Request for the financial-assistance application-eligibility check. The applicant is mandatory; a co-applicant
 * present
 * here means the citizen is applying together with a partner (medsökande), which is matched against the constellation
 * of
 * any earlier application/decision.
 */
@Schema(description = "Request to evaluate which financial assistance application a citizen should be offered.")
public class EligibilityRequest {

	@Schema(description = "The applicant's personal number (12 digits)", examples = "198001012389", requiredMode = Schema.RequiredMode.REQUIRED)
	@ValidPersonalNumber
	private String applicant;

	@Schema(description = "The co-applicant's (medsökande) personal number (12 digits), when applying together with a partner", examples = "198202022397")
	@ValidPersonalNumber(nullable = true)
	private String coApplicant;

	@Schema(description = "Override for the duplicate-application window in days. Defaults to the server-configured value when omitted.", examples = "90")
	@Positive
	private Integer withinDays;

	public static EligibilityRequest create() {
		return new EligibilityRequest();
	}

	public String getApplicant() {
		return applicant;
	}

	public void setApplicant(final String applicant) {
		this.applicant = applicant;
	}

	public EligibilityRequest withApplicant(final String applicant) {
		this.applicant = applicant;
		return this;
	}

	public String getCoApplicant() {
		return coApplicant;
	}

	public void setCoApplicant(final String coApplicant) {
		this.coApplicant = coApplicant;
	}

	public EligibilityRequest withCoApplicant(final String coApplicant) {
		this.coApplicant = coApplicant;
		return this;
	}

	public Integer getWithinDays() {
		return withinDays;
	}

	public void setWithinDays(final Integer withinDays) {
		this.withinDays = withinDays;
	}

	public EligibilityRequest withWithinDays(final Integer withinDays) {
		this.withinDays = withinDays;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final EligibilityRequest that = (EligibilityRequest) o;
		return Objects.equals(applicant, that.applicant) && Objects.equals(coApplicant, that.coApplicant)
			&& Objects.equals(withinDays, that.withinDays);
	}

	@Override
	public int hashCode() {
		return Objects.hash(applicant, coApplicant, withinDays);
	}

	@Override
	public String toString() {
		return "EligibilityRequest{applicant='" + applicant + "', coApplicant='" + coApplicant + "', withinDays=" + withinDays + '}';
	}
}
