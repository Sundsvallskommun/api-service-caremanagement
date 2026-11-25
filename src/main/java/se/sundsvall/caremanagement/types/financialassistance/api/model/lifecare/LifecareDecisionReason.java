package se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

/**
 * An orsak a beslut of a beslutstyp can carry, as Lifecare words it, under the heading it sits in.
 *
 * @param code   Lifecare's reasonCode
 * @param name   the orsak
 * @param header the heading it sits under
 */
@Schema(description = "An orsak a beslut of the beslutstyp can carry, as Lifecare words it, under the heading it sits in.", accessMode = READ_ONLY)
public record LifecareDecisionReason(
	@Schema(description = "Lifecare's reasonCode", examples = "19") Integer code,
	@Schema(description = "The orsak as Lifecare words it", examples = "Arbetar deltid ofrivilligt, otillräcklig inkomst") String name,
	@Schema(description = "The heading the orsak sits under", examples = "Arbetar deltid, ofrivilligt") String header) {
}
