package se.sundsvall.caremanagement.document.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Optional request body for locking a document (write-protection). Carries who locked it; the body may be omitted
 * entirely.
 */
public record LockDocument(

	@Schema(description = "User id of whoever locks the document; optional", example = "carola01winberg") @Size(max = 64) String lockedBy) {}
