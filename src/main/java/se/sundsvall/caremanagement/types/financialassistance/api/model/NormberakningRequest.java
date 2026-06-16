package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

/**
 * Request to build and post an SSBTEK-driven normberäkning to Lifecare FC for one applicant (and optional co-applicant)
 * and one application month. The household's income basis is fetched from SSBTEK for the rule periods derived from the
 * month; the resulting normberäkning is created in Lifecare.
 */
@Schema(description = "Request to build and post the SSBTEK-driven normberäkning for an application month.")
public class NormberakningRequest {

	@Schema(description = "The applicant's partyId (personId GUID)", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479", requiredMode = Schema.RequiredMode.REQUIRED)
	@ValidUuid
	private String applicant;

	@Schema(description = "The co-applicant's (medsökande) partyId (personId GUID), when applying together with a partner", examples = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
	@ValidUuid(nullable = true)
	private String coApplicant;

	@Schema(description = "The application month (ISO year-month, yyyy-MM)", examples = "2026-06", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "must be an ISO year-month (yyyy-MM)")
	private String applicationMonth;

	public static NormberakningRequest create() {
		return new NormberakningRequest();
	}

	public String getApplicant() {
		return applicant;
	}

	public void setApplicant(final String applicant) {
		this.applicant = applicant;
	}

	public NormberakningRequest withApplicant(final String applicant) {
		this.applicant = applicant;
		return this;
	}

	public String getCoApplicant() {
		return coApplicant;
	}

	public void setCoApplicant(final String coApplicant) {
		this.coApplicant = coApplicant;
	}

	public NormberakningRequest withCoApplicant(final String coApplicant) {
		this.coApplicant = coApplicant;
		return this;
	}

	public String getApplicationMonth() {
		return applicationMonth;
	}

	public void setApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
	}

	public NormberakningRequest withApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final NormberakningRequest that = (NormberakningRequest) o;
		return Objects.equals(applicant, that.applicant) && Objects.equals(coApplicant, that.coApplicant) && Objects.equals(applicationMonth, that.applicationMonth);
	}

	@Override
	public int hashCode() {
		return Objects.hash(applicant, coApplicant, applicationMonth);
	}

	@Override
	public String toString() {
		return "NormberakningRequest{applicant='" + applicant + "', coApplicant='" + coApplicant + "', applicationMonth='" + applicationMonth + "'}";
	}
}
