package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * The errand's beslut as it stands in Lifecare, cleaned up for the Beslut tab and for finalize. The personnummer of the
 * persons it concerns is left behind.
 *
 * @param id            Lifecare's decisionId
 * @param decisionCode  Lifecare's beslutstyp code
 * @param outcome       careM's outcome for the beslutstyp, null for a type careM does not register
 * @param date          the beslutsdatum
 * @param periodFrom    start of the period, null when none
 * @param periodTo      end of the period, null when none
 * @param amount        the amount, 0 when none
 * @param reasonCode    Lifecare's orsak code, null when none
 * @param reason        the orsak as Lifecare words it
 * @param message       the beslutsmeddelande as HTML
 * @param locked        whether Lifecare has locked the beslutsmeddelande
 * @param decisionMaker the beslutsfattare as Lifecare names it
 */
@Schema(description = "The errand's beslut as it stands in Lifecare - what the Beslut tab shows and what finalize records.", accessMode = READ_ONLY)
public record LifecareDecisionView(
	@Schema(description = "Lifecare's decisionId", examples = "98") Integer id,
	@Schema(description = "Lifecare's beslutstyp code", examples = "153") Integer decisionCode,
	@Schema(description = "careM's outcome for the beslutstyp, BIFALL or AVSLAG; absent for a beslutstyp that cannot be registered from careM",
		examples = "BIFALL",
		allowableValues = {
			"BIFALL", "AVSLAG"
		}) String outcome,
	@Schema(description = "The beslutsdatum, yyyy-MM-dd; empty when Lifecare has none", examples = "2026-09-23") String date,
	@Schema(description = "Start of the period the beslut covers, yyyy-MM-dd", examples = "2026-09-01") String periodFrom,
	@Schema(description = "End of the period the beslut covers, yyyy-MM-dd", examples = "2026-09-30") String periodTo,
	@Schema(description = "The amount, 0 when none", examples = "3000") BigDecimal amount,
	@Schema(description = "Lifecare's code for the orsak; absent when the beslut has none", examples = "19") Integer reasonCode,
	@Schema(description = "The orsak as Lifecare words it", examples = "Arbetar deltid ofrivilligt, otillräcklig inkomst") String reason,
	@Schema(description = "The beslutsmeddelande as HTML", examples = "<p>Beslut</p>") String message,
	@Schema(description = "Lifecare has locked the beslutsmeddelande; the beslut can no longer be changed from careM", examples = "false") boolean locked,
	@Schema(description = "The beslutsfattare, by name when Lifecare gives one, otherwise by signature", examples = "Test Handläggare") String decisionMaker) {
}
