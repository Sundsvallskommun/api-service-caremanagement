package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

/**
 * Request to create the Lifecare FC aktualisering (case intake) for one applicant and one application month. The
 * aktualisering is built against the applicant's FC aktualisering proposal and created in Lifecare; the intake date is
 * the first day of the application month.
 */
@Schema(description = "Request to create the Lifecare aktualisering (case intake) for an application month.")
public class ActualisationRequest {

	@Schema(description = "The applicant's partyId (personId GUID)", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479", requiredMode = Schema.RequiredMode.REQUIRED)
	@ValidUuid
	private String applicant;

	@Schema(description = "The application month (ISO year-month, yyyy-MM); the aktualisering's intake date is the first day of this month", examples = "2026-06", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "must be an ISO year-month (yyyy-MM)")
	private String applicationMonth;

	@Schema(
		description = "The id of the caremanagement errand the aktualisering concerns. When present, a Decision(ACTUALISATION) recording the created Lifecare aktualisering id is added to the errand's audit trail; when omitted, the aktualisering is created without recording anything on an errand.",
		examples = "cb20c51f-fcf3-42c0-b613-de563634a8ec")
	@ValidUuid(nullable = true)
	private String errandId;

	public static ActualisationRequest create() {
		return new ActualisationRequest();
	}

	public String getApplicant() {
		return applicant;
	}

	public void setApplicant(final String applicant) {
		this.applicant = applicant;
	}

	public ActualisationRequest withApplicant(final String applicant) {
		this.applicant = applicant;
		return this;
	}

	public String getApplicationMonth() {
		return applicationMonth;
	}

	public void setApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
	}

	public ActualisationRequest withApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
		return this;
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public ActualisationRequest withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final ActualisationRequest that = (ActualisationRequest) o;
		return Objects.equals(applicant, that.applicant) && Objects.equals(applicationMonth, that.applicationMonth) && Objects.equals(errandId, that.errandId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(applicant, applicationMonth, errandId);
	}

	@Override
	public String toString() {
		return "ActualisationRequest{applicant='" + applicant + "', applicationMonth='" + applicationMonth + "', errandId='" + errandId + "'}";
	}
}
