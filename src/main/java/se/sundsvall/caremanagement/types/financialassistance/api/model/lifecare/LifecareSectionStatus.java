package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * Which of the errand's sections Lifecare has as done: the checks on the Normberäkning, Beslut and Utbetalning tabs.
 *
 * @param calculationFinalized the errand's beräkning is saved as slutlig in Lifecare
 * @param decisionSaved        the errand's beslut is saved in Lifecare
 * @param paymentRegistered    an utbetalning for the errand's month is registered in Lifecare
 */
@Schema(description = "Which of the errand's sections Lifecare has as done - the checks on the Normberäkning, Beslut and Utbetalning tabs.", accessMode = READ_ONLY)
public record LifecareSectionStatus(
	@Schema(description = "The errand's beräkning is saved as slutlig in Lifecare", examples = "true") boolean calculationFinalized,
	@Schema(description = "The errand's beslut is saved in Lifecare", examples = "true") boolean decisionSaved,
	@Schema(description = "An utbetalning for the errand's month is registered in Lifecare", examples = "false") boolean paymentRegistered) {
}
