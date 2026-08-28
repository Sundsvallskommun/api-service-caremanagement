package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The context an RPA robot needs to act on an errand in Lifecare — fetched as the robot's first step after picking up a
 * queue item, instead of the personal numbers riding in the queue item itself. Every read of this resource lands in the
 * errand's event log with the caller's {@code X-Sent-By} identity, so each personal-number disclosure is traceable.
 */
@Schema(description = "The context an RPA robot needs to act on an errand in Lifecare. Fetched per queue item so personal numbers "
	+ "never persist in the Orchestrator queue store; every read is recorded in the errand's event log.")
public record RpaContext(

	@Schema(description = "The errand's human-readable number — what a person searches for in Draken", examples = "EB-2026-000123") String errandNumber,

	@Schema(description = "The applicant's personal number (12 characters, may contain letters). Null when it could not be resolved — "
		+ "treat as an error on the robot side.", examples = "19800101T001") String applicantPersonId,

	@Schema(description = "The co-applicant's personal number; null when there is no co-applicant or it could not be resolved", examples = "19850505T002") String coApplicantPersonId) {}
