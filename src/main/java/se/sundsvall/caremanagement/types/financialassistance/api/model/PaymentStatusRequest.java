package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;

/**
 * Request to read whether the manual Lifecare utbetalning for one applicant and one application month has been
 * effectuated. The process polls this after a bifall to detect when the handläggare's manual payment is registered in
 * Lifecare — caremanagement makes no payment.
 */
@Schema(description = "Request to read whether the Lifecare utbetalning for an application month has been effectuated.")
public class PaymentStatusRequest {

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
		return Objects.equals(applicant, that.applicant) && Objects.equals(applicationMonth, that.applicationMonth);
	}

	@Override
	public int hashCode() {
		return Objects.hash(applicant, applicationMonth);
	}

	@Override
	public String toString() {
		return "PaymentStatusRequest{applicant='" + applicant + "', applicationMonth='" + applicationMonth + "'}";
	}
}
