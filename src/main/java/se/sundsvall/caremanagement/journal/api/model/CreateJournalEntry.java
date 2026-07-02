package se.sundsvall.caremanagement.journal.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Request to create a journalanteckning. Mirrors the Lifecare "Ny journalanteckning" form — Typ, Rubrik and Datum are
 * required, Tid and the body text are optional. The entry is created in the editable WORKING state.
 */
public record CreateJournalEntry(

	@Schema(description = "Journal entry type (Lifecare 'Typ'/Journaltyp)", example = "Journalfört meddelande", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String type,

	@Schema(description = "Heading (Lifecare 'Rubrik')", example = "Journalfört meddelande: 2025-05-30 Info", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String heading,

	@Schema(description = "Free-text body of the journal entry; optional", example = "Hej! Vill bara informera att jag fått jobb på Mejeriet.") @Size(max = 1_048_576) String text,

	@Schema(description = "Documented date and time (Lifecare 'Datum'/'Tid')", example = "2025-05-30T14:30:00+02:00", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull OffsetDateTime entryDateTime,

	@Schema(description = "User id of the author (Lifecare 'Upprättad av'); optional", example = "carola01winberg") @Size(max = 64) String createdBy) {}
