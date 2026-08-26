package se.sundsvall.caremanagement.types.financialassistance.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One row from Lifecare's document list — the shape both journal notes and regular documents arrive in (Lifecare's
 * "Dokumentation" container holds both). {@code documentType} discriminates: {@code 3} (JournalNote) routes to the
 * journal module, {@code 0} (Regular) to the document module; other values are reported SKIPPED. The row's {@code id}
 * is the upsert key — Lifecare ids are unique only within their type, so the key is effectively
 * {@code (documentType, id)}. All scalars are received as strings for tolerance.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "One row from Lifecare's document list — journal notes (documentType 3) and regular documents (documentType 0) share this shape.")
public record LifecareDocumentRow(

	@Schema(description = "The row's id in Lifecare's document list — the upsert key together with documentType. Rows without it are skipped.", examples = "27") String id,

	@Schema(description = "Title (Lifecare 'Rubrik'); becomes the mirrored heading", examples = "Journalanteckning") String title,

	@Schema(description = "Documented date (yyyy-MM-dd). Required — rows without a parseable date are reported FAILED.", examples = "2026-08-05") String date,

	@Schema(description = "Documented time (HH:mm); optional, midnight when absent", examples = "11:06") String time,

	@Schema(description = "Type display text (Lifecare 'Typ'); becomes the mirrored type, falling back to typeCode", examples = "Journalanteckning") String type,

	@Schema(description = "Type code (Lifecare notes: 1 Journalanteckning; documents: e.g. 13 BE Brev, 14 BE Dokument)", examples = "1") String typeCode,

	@Schema(description = "Row discriminator: 3 = journal note, 0 = regular document. Other values are reported SKIPPED.", examples = "3") String documentType,

	@Schema(description = "Body as Lifecare returns it — HTML with entities. Decoded and stripped to plain text before storage.", examples = "<p>Hej! Vill bara informera att jag f&aring;tt jobb.</p>") String content,

	@Schema(description = "The signature of the last writer in Lifecare; becomes the mirrored author", examples = "carola01winberg") String updateSignature,

	@Schema(description = "Lifecare's last-update date (yyyy-MM-dd, day precision only); informational", examples = "2026-08-05") String updateDate,

	@Schema(description = "Lifecare's textual name for documentType; informational", examples = "JournalNote") @JsonProperty("documentType_Name") String documentTypeName) {}
