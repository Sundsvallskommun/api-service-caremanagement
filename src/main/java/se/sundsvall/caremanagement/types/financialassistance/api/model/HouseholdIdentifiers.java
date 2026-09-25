package se.sundsvall.caremanagement.types.financialassistance.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The errand number and the household's personal numbers, fetched per run by the Operaton process's beredning step
 * ({@code prepare-income-basis}) so the personal numbers never become process variables.
 * Every read of this resource lands in the errand's event log with the caller's
 * {@code X-Sent-By} identity, so each personal-number disclosure is traceable.
 */
@Schema(description = "The errand number and the household's personal numbers. Fetched per process run so the personal numbers "
	+ "never become process variables; every read is recorded in the errand's event log.")
public record HouseholdIdentifiers(

	@Schema(description = "The errand's human-readable number — what a person searches for in Draken", examples = "EB-2026-000123") String errandNumber,

	@Schema(description = "The applicant's personal number (12 characters, may contain letters). Null when it could not be resolved — "
		+ "treat as an error on the caller's side.", examples = "19800101T001") String applicantPersonId,

	@Schema(description = "The co-applicant's personal number; null when there is no co-applicant or it could not be resolved", examples = "19850505T002") String coApplicantPersonId,

	@Schema(description = "The household children named on the application that have a partyId; empty when there are none") List<HouseholdChild> children) {}
