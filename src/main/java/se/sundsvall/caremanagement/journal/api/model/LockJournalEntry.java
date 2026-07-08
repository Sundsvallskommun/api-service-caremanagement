package se.sundsvall.caremanagement.journal.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Optional request body for locking a journal entry (write-protection). Carries who locked it; the body may be omitted
 * entirely.
 */
public record LockJournalEntry(

	@Schema(description = "User id of whoever locks the entry; optional", examples = "carola01winberg") @Size(max = 64) String lockedBy) {}
