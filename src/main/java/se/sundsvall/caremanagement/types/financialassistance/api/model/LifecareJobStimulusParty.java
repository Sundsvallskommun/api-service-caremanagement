package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One party's jobbstimulans periods in the {@code GetJobStimulusForService} response. Person identity fields present in
 * the Lifecare response are deliberately not modelled — the errand already knows its client.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "One party's jobbstimulans periods.")
public record LifecareJobStimulusParty(

	@Schema(description = "The party's periods") List<LifecareJobStimulusPeriod> periods) {}
