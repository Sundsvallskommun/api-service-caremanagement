package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * A beslut as the caseworker saves it on the Beslut tab. It is written straight to Lifecare: created the first time,
 * changed after that. Beslutstyp and orsak are Lifecare's own codes, picked from its lists.
 *
 * @param decisionCode          Lifecare's beslutstyp code
 * @param date                  the beslutsdatum, Lifecare's proposal (today) when left out
 * @param periodFrom            start of the period
 * @param periodTo              end of the period
 * @param amount                the amount
 * @param reasonCode            Lifecare's orsak code
 * @param decisionMessage       the beslutsmeddelande as HTML
 * @param writeProtect          save the beslut write-protected (its meddelande locked)
 * @param coApplicantReasonCode Lifecare's orsak code for the medsökande
 */
@Schema(description = "A beslut as the caseworker saves it on the Beslut tab - written straight to Lifecare, created the first time and changed after that.")
public record LifecareDecisionSaveRequest(
	@Schema(description = "Lifecare's beslutstyp code, from the beslutstyper the insats offers", examples = "153", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Integer decisionCode,
	@Schema(description = "The beslutsdatum; Lifecare's proposal (today) when left out", examples = "2026-09-23") @DateTimeFormat(iso = DATE) LocalDate date,
	@Schema(description = "Start of the period the beslut covers; the normberäkning's when left out and the errand's normberäkning is saved as final", examples = "2026-09-01") @DateTimeFormat(iso = DATE) LocalDate periodFrom,
	@Schema(description = "End of the period the beslut covers; the normberäkning's when left out and the errand's normberäkning is saved as final", examples = "2026-09-30") @DateTimeFormat(iso = DATE) LocalDate periodTo,
	@Schema(description = "The amount; when left out, the result of the errand's normberäkning if it is saved as final, otherwise 0", examples = "3000") BigDecimal amount,
	@Schema(description = "Lifecare's code for the applicant's orsak, from the orsaker of the beslutstyp", examples = "19") Integer reasonCode,
	@Schema(description = "The beslutsmeddelande as HTML", examples = "<p>Beslut</p>") @Size(max = 1048576) String decisionMessage,
	@Schema(description = """
		Spara och skrivskydda beslut: saves the beslut write-protected in Lifecare (its meddelande locked), after which it \
		can no longer be changed from careM.""", examples = "false") Boolean writeProtect,
	@Schema(description = """
		Lifecare's code for the medsökande's orsak, from the same orsaker as the sökande's. Required when the household has \
		a medsökande in Lifecare and the beslut carries an orsak; ignored without a medsökande.""", examples = "1") Integer coApplicantReasonCode) {
}
