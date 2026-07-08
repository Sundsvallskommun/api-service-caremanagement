package se.sundsvall.caremanagement.journal.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Request to update a journalanteckning. Replaces the editable fields (Typ, Rubrik, text, Datum, Tid) and records the
 * editor. Only allowed while the entry is WORKING — a LOCKED entry (upprättad handling) is immutable and rejects this
 * with {@code 409 Conflict}.
 */
public record UpdateJournalEntry(

	@Schema(description = "Journal entry type (Lifecare 'Typ'/Journaltyp)", examples = "Journalfört meddelande", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String type,

	@Schema(description = "Heading (Lifecare 'Rubrik')", examples = "Journalfört meddelande: 2025-05-30 Info", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String heading,

	@Schema(description = "Free-text body of the journal entry; optional", examples = "Hej! Vill bara informera att jag fått jobb på Mejeriet.") @Size(max = 1_048_576) String text,

	@Schema(description = "Documented date and time (Lifecare 'Datum'/'Tid')", examples = "2025-05-30T14:30:00+02:00", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull OffsetDateTime entryDateTime,

	@Schema(description = "User id of the editor (Lifecare 'Ändrat av'); optional", examples = "ebb14eri") @Size(max = 64) String modifiedBy) {}
