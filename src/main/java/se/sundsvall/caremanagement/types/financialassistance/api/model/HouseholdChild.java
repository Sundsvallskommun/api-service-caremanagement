package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One household child named on the application, as the beredning needs it to read the child's SSBTEK basis: the partyId
 * it tags the child's incomes with, and the personal number SSBTEK is asked with.
 */
@Schema(description = "A household child named on the application: the partyId the beredning tags the child's SSBTEK incomes with, "
	+ "and the personal number SSBTEK is read with.")
public record HouseholdChild(

	@Schema(description = "The child's partyId, as on the errand's children", examples = "5b1e7c3a-9d2f-4e8b-a6c4-1f0d2e3c4b5a") String partyId,

	@Schema(description = "The child's personal number (12 characters, may contain letters); null when it could not be resolved",
		examples = "20100101T003") String personId) {}
