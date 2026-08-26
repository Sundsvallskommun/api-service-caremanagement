package se.sundsvall.caremanagement.shared;

/**
 * The result of mirroring an external (Lifecare) record onto an errand — the local id of the mirrored row and whether
 * the upsert created it or refreshed an existing mirror. Returned by the journal and document mirror operations so the
 * caller (the financial assistance supplements ingest) can report CREATED vs UPDATED per delivered item.
 */
public record MirrorOutcome(String id, boolean created) {}
