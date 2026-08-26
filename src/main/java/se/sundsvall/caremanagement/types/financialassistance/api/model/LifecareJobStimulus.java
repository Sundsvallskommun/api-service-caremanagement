package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The response from Lifecare's {@code GetJobStimulusForService}, trimmed to the period sets. {@code coApplicant} is
 * tolerated in both captured variants — {@code null} and an empty object with no periods mean the same thing: no
 * co-applicant periods.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "The jobbstimulans periods from Lifecare's GetJobStimulusForService — the applicant's and, when present, the co-applicant's.")
public record LifecareJobStimulus(

	@Schema(description = "The applicant's period set") LifecareJobStimulusParty applicant,

	@Schema(description = "The co-applicant's period set; null or an empty object when there is no co-applicant") LifecareJobStimulusParty coApplicant) {}
