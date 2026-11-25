package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * A beslutstyp the errand's insats offers in Lifecare.
 *
 * @param code             Lifecare's beslutstyp code
 * @param name             the beslutstyp's name
 * @param outcome          careM's outcome for it, null for a type careM does not register
 * @param requiresFromDate whether a beslut of the type needs a from date
 * @param requiresToDate   whether a beslut of the type needs a to date
 */
@Schema(description = "A beslutstyp the errand's insats offers in Lifecare, as the Beslut tab lists it.", accessMode = READ_ONLY)
public record LifecareDecisionType(
	@Schema(description = "Lifecare's beslutstyp code", examples = "153") Integer code,
	@Schema(description = "The beslutstyp's name", examples = "Ek Ekonomiskt bistånd 12 kap 1, 7 §§ SoL, bifall") String name,
	@Schema(description = "careM's outcome for the type, BIFALL or AVSLAG; absent for a type that cannot be registered from careM yet",
		examples = "BIFALL",
		allowableValues = {
			"BIFALL", "AVSLAG"
		}) String outcome,
	@Schema(description = "Whether a beslut of the type requires a from date", examples = "true") boolean requiresFromDate,
	@Schema(description = "Whether a beslut of the type requires a to date", examples = "true") boolean requiresToDate) {
}
