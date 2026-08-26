package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One bevakning row as Lifecare's {@code ListRemindersByServiceId} returns it. Codes arrive as their numeric value with
 * the display text as a nullable companion (the text is not always populated — the code is the truth). All scalars are
 * received as strings for tolerance; person fields present in the Lifecare response are deliberately not modelled — the
 * errand already knows its client.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "One bevakning row as Lifecare's ListRemindersByServiceId returns it. The stable reminderId is the upsert key.")
public record LifecareReminder(

	@Schema(description = "Lifecare's stable reminder id — the idempotency key. Rows without it are skipped.", examples = "4") String reminderId,

	@Schema(description = "The monitoring date (yyyy-MM-dd). Required — rows without a parseable date are reported FAILED.", examples = "2026-10-17") String reminderDate,

	@Schema(description = "Status code (Lifecare: 1 Pågår, 2 Klar, 3 Ej påbörjad, 4 Väntar — a snapshot, not a definition)", examples = "1") String status,

	@Schema(description = "Status display text; may be null", examples = "Pågår") String statusText,

	@Schema(description = "Priority code (Lifecare: 1 Hög, 2 Normal, 3 Låg)", examples = "2") String priority,

	@Schema(description = "Priority display text; may be null", examples = "Normal") String priorityText,

	@Schema(description = "Reminder type code", examples = "3") String type,

	@Schema(description = "Reminder type display text; may be null", examples = "Manuell bevakning insats") String typeText,

	@Schema(description = "The caseworker's free text", examples = "Följ upp inkomstuppgifter från CSN") String text,

	@Schema(description = "The caseworker id in Lifecare", examples = "TEST") String caseworkerId,

	@Schema(description = "The caseworker's display name", examples = "Test Handläggare") String caseworkerName,

	@Schema(description = "What the reminder sits on (Lifecare: 7083 IFO.Insats, 7040 IFO.Aktualisering)", examples = "7083") String objectType,

	@Schema(description = "Object type display name", examples = "IFO.Insats") String objectTypeName) {}
