package se.sundsvall.caremanagement.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request to create a document. Mirrors the Lifecare document form — Typ, Rubrik and Datum are required, Tid and the
 * body text are optional. The document is created in the editable WORKING state.
 */
public record CreateDocument(

	@Schema(description = "Document type (Lifecare 'Typ'/Dokumenttyp)", examples = "Brev", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String type,

	@Schema(description = "Heading (Lifecare 'Rubrik')", examples = "Beslut om ekonomiskt bistånd 2025-05", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String heading,

	@Schema(description = "Free-text body of the document; optional", examples = "Beslut har fattats enligt nedan ...") @Size(max = 1_048_576) String text,

	@Schema(description = "Documented date (Lifecare 'Datum')", examples = "2025-05-30", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull LocalDate documentDate,

	@Schema(description = "Documented time (Lifecare 'Tid'); optional", examples = "14:30") LocalTime documentTime,

	@Schema(description = "User id of the author (Lifecare 'Upprättad av'); optional", examples = "carola01winberg") @Size(max = 64) String createdBy) {}
