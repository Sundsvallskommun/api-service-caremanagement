package se.sundsvall.caremanagement.journal.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request to update a journalanteckning. Replaces the editable fields (Typ, Rubrik, text, Datum, Tid) and records the
 * editor. Only allowed while the entry is WORKING — a LOCKED entry (upprättad handling) is immutable and rejects this
 * with {@code 409 Conflict}.
 */
public record UpdateJournalEntry(

	@Schema(description = "Journal entry type (Lifecare 'Typ'/Journaltyp)", example = "Journalfört meddelande", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String type,

	@Schema(description = "Heading (Lifecare 'Rubrik')", example = "Journalfört meddelande: 2025-05-30 Info", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String heading,

	@Schema(description = "Free-text body of the journal entry; optional", example = "Hej! Vill bara informera att jag fått jobb på Mejeriet.") @Size(max = 1_048_576) String text,

	@Schema(description = "Documented date (Lifecare 'Datum')", example = "2025-05-30", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull LocalDate entryDate,

	@Schema(description = "Documented time (Lifecare 'Tid'); optional", example = "14:30") LocalTime entryTime,

	@Schema(description = "User id of the editor (Lifecare 'Ändrat av'); optional", example = "ebb14eri") @Size(max = 64) String modifiedBy) {}
