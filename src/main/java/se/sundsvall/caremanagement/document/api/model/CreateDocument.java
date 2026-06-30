package se.sundsvall.caremanagement.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request to create a Dokument. Mirrors the Lifecare document form — Typ, Rubrik and Datum are required, Tid and the
 * body text are optional. The document is created in the editable WORKING state.
 */
public record CreateDocument(

	@Schema(description = "Document type (Lifecare 'Typ'/Dokumenttyp)", example = "Brev", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String type,

	@Schema(description = "Heading (Lifecare 'Rubrik')", example = "Beslut om ekonomiskt bistånd 2025-05", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String heading,

	@Schema(description = "Free-text body of the document; optional", example = "Beslut har fattats enligt nedan ...") @Size(max = 1_048_576) String text,

	@Schema(description = "Documented date (Lifecare 'Datum')", example = "2025-05-30", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull LocalDate documentDate,

	@Schema(description = "Documented time (Lifecare 'Tid'); optional", example = "14:30") LocalTime documentTime,

	@Schema(description = "User id of the author (Lifecare 'Upprättad av'); optional", example = "carola01winberg") @Size(max = 64) String createdBy) {}
