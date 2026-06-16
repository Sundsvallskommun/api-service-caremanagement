package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

/**
 * Request for the financial-assistance application-eligibility check. The applicant is mandatory; a co-applicant
 * present
 * here means the citizen is applying together with a partner (medsökande), which is matched against the constellation
 * of
 * any earlier application/decision.
 */
@Schema(description = "Request to evaluate which financial assistance application a citizen should be offered.")
public class EligibilityRequest {

	@Schema(description = "The applicant's partyId (personId GUID)", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479", requiredMode = Schema.RequiredMode.REQUIRED)
	@ValidUuid
	private String applicant;

	@Schema(description = "The co-applicant's (medsökande) partyId (personId GUID), when applying together with a partner", examples = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
	@ValidUuid(nullable = true)
	private String coApplicant;

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

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final EligibilityRequest that = (EligibilityRequest) o;
		return Objects.equals(applicant, that.applicant) && Objects.equals(coApplicant, that.coApplicant);
	}

	@Override
	public int hashCode() {
		return Objects.hash(applicant, coApplicant);
	}

	@Override
	public String toString() {
		return "EligibilityRequest{applicant='" + applicant + "', coApplicant='" + coApplicant + "'}";
	}
}
