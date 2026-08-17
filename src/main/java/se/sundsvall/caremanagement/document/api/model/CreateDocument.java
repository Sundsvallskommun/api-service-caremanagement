package se.sundsvall.caremanagement.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Request to create a document. Mirrors the Lifecare document form — Typ, Rubrik and Datum/Tid are required, the body
 * text is optional. The document is created in the editable WORKING state.
 */
public record CreateDocument(

	@Schema(description = "Document type (Lifecare 'Typ'/Dokumenttyp)", examples = "Brev", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String type,

	@Schema(description = "Heading (Lifecare 'Rubrik')", examples = "Beslut om ekonomiskt bistånd 2025-05", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 255) String heading,

	@Schema(description = "Free-text body of the document; optional", examples = "Beslut har fattats enligt nedan ...") @Size(max = 1_048_576) String text,

	@Schema(description = "Documented date and time (Lifecare 'Datum'/'Tid')", examples = "2025-05-30T14:30:00+02:00", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull OffsetDateTime documentDateTime,

	@Schema(description = "User id of the author (Lifecare 'Upprättad av'); optional", examples = "carola01winberg") @Size(max = 64) String createdBy) {}
