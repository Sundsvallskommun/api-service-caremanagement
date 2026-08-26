package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The RPA supplements delivery envelope — one near-raw dump of everything the robot read out of Lifecare Professional
 * Web for the errand's client. Sections are named and shaped after the Lifecare responses so the robot delivers what it
 * captured with minimal transformation; CareManagement does the routing, mapping and idempotency. An omitted section
 * means 'not fetched this run' and leaves the errand untouched; an empty section means 'fetched, nothing there'.
 * Unknown fields are ignored throughout.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "RPA supplements delivery envelope — a near-raw dump of the Lifecare Professional Web responses for the errand's client. "
	+ "An omitted section means 'not fetched this run'; an empty section means 'fetched, nothing there'. Unknown fields are ignored.")
public record LifecareSupplements(

	@Schema(description = "The robot's capture date (day precision — Lifecare's own timestamps carry no more)", examples = "2026-08-25") String capturedAt,

	@Schema(description = "Rows from Lifecare's ListRemindersByServiceId (bevakningar). Upserted as LIFECARE-sourced monitorings on the errand, keyed per reminderId.") List<LifecareReminder> reminders,

	@Schema(description = "Rows from Lifecare's document list — journal notes (documentType 3) and regular documents (documentType 0) alike. "
		+ "CareManagement routes each row on documentType; the robot does not need to tell them apart.") List<LifecareDocumentRow> documents,

	@Schema(description = "The response from Lifecare's GetJobStimulusForService. Replaces the errand's full jobbstimulans period set — "
		+ "Lifecare regenerates all period ids on every save, so ids are never used as keys.") LifecareJobStimulus jobStimulus) {}
