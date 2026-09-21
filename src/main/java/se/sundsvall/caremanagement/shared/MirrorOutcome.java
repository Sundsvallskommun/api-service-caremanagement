package se.sundsvall.caremanagement.shared;

/**
 * The result of mirroring an external (Lifecare) record onto an errand — the local id of the mirrored row, whether the
 * upsert created it, and whether it actually altered anything. Returned by the journal and document mirror operations
 * so the caller (the financial assistance supplements ingest) can report CREATED, UPDATED or UNCHANGED per delivered
 * item. A re-delivery that carries the same content as the stored mirror is {@code changed = false} and leaves the
 * row's modification stamp alone, so "last modified" tracks Lifecare's changes rather than the robot's polling.
 */
public record MirrorOutcome(String id, boolean created, boolean changed) {}
