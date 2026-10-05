package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

/**
 * Request to read whether the Lifecare payments of a bifall have been effectuated. The process polls this after the
 * decision to detect when the payments are registered in Lifecare — caremanagement makes no payment. With
 * {@code errandId}, exactly the payments the errand's decision registered are verified; without it, any payment for the
 * applicant and month counts.
 */
@Schema(description = "Request to read whether the Lifecare payment for an application month has been effectuated.")
public class PaymentStatusRequest {

	@Schema(description = """
		The errand whose payments to verify. When given, the check is errand-specific: the Lifecare payments linked to the
		errand (lifecarePaymentIds), else the applicant's Lifecare payments on the errand's own insats for the application month that no other errand references. Without it, any
		Lifecare payment for the applicant and application month counts — kept only for callers that predate the field.""", examples = "a3c1f4de-2b6a-4c1e-9d3f-7e8a9b0c1d2e")
	@ValidUuid(nullable = true)
	private String errandId;

	@Schema(description = "The applicant's partyId (personId GUID)", examples = "f47ac10b-58cc-4372-a567-0e02b2c3d479", requiredMode = Schema.RequiredMode.REQUIRED)
	@ValidUuid
	private String applicant;

	@Schema(description = "The application month (ISO year-month, yyyy-MM) the payment concerns", examples = "2026-06", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "must be an ISO year-month (yyyy-MM)")
	private String applicationMonth;

	public static PaymentStatusRequest create() {
		return new PaymentStatusRequest();
	}

	public String getErrandId() {
		return errandId;
	}

	public void setErrandId(final String errandId) {
		this.errandId = errandId;
	}

	public PaymentStatusRequest withErrandId(final String errandId) {
		this.errandId = errandId;
		return this;
	}

	public String getApplicant() {
		return applicant;
	}

	public void setApplicant(final String applicant) {
		this.applicant = applicant;
	}

	public PaymentStatusRequest withApplicant(final String applicant) {
		this.applicant = applicant;
		return this;
	}

	public String getApplicationMonth() {
		return applicationMonth;
	}

	public void setApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
	}

	public PaymentStatusRequest withApplicationMonth(final String applicationMonth) {
		this.applicationMonth = applicationMonth;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || getClass() != o.getClass())
			return false;
		final PaymentStatusRequest that = (PaymentStatusRequest) o;
		return Objects.equals(errandId, that.errandId) && Objects.equals(applicant, that.applicant) && Objects.equals(applicationMonth, that.applicationMonth);
	}

	@Override
	public int hashCode() {
		return Objects.hash(errandId, applicant, applicationMonth);
	}

	@Override
	public String toString() {
		return "PaymentStatusRequest{errandId='" + errandId + "', applicant='" + applicant + "', applicationMonth='" + applicationMonth + "'}";
	}
}
