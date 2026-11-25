package se.sundsvall.caremanagement.types.financialassistance.service.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the recovery of a financial assistance process start that did not take (see
 * {@code FinancialAssistanceProcessStartRetryWorker}). An errand still in status {@code RECEIVED}, with no process
 * instance linked, somewhere between {@code minAge} and {@code maxAge} after it was created, is given its process
 * again.
 *
 * @param enabled        whether the job does anything. On by default; switching it off leaves an errand whose start
 *                       failed where it is
 * @param municipalityId the municipality whose errands are recovered
 * @param namespace      the namespace whose errands are recovered (financial assistance =
 *                       {@code FINANCIAL_ASSISTANCE})
 * @param minAge         how old an errand must be before the job touches it — long enough that it never races the
 *                       start that follows the errand's creation
 * @param maxAge         how old an errand may be and still be retried. Bounds the retrying (a start that keeps failing
 *                       is given up on at this age, and logged as an error) and keeps old rows from being woken up
 */
@Validated
@ConfigurationProperties(prefix = "financial-assistance.process-start-retry")
public record FinancialAssistanceProcessStartRetryProperties(

	@DefaultValue("true") boolean enabled,

	@NotBlank @DefaultValue("2281") String municipalityId,

	@NotBlank @DefaultValue("FINANCIAL_ASSISTANCE") String namespace,

	@NotNull @DefaultValue("PT10M") Duration minAge,

	@NotNull @DefaultValue("P7D") Duration maxAge) {
}
