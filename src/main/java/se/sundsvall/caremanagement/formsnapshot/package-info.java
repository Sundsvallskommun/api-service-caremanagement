/**
 * Form-snapshot module — an immutable, self-describing record of a citizen-facing application form exactly as it was
 * rendered and answered (every question, help/info/notice text, option label and answer), authored by the frontend at
 * submission time and stored verbatim with a SHA-256 content hash.
 *
 * <p>
 * Domain-agnostic and universal across errand types (the same altitude as {@code attachments} / {@code journal} /
 * {@code eventlog}): a type module wires in the exposed {@code service} to capture a snapshot when its errand is
 * created and to read it back for re-display. Write-once — one snapshot per errand, no update path — and removed when
 * the errand is deleted via an {@code ErrandDeleted} listener, so its retention follows the case.
 * </p>
 */
@ApplicationModule(displayName = "Form Snapshot")
package se.sundsvall.caremanagement.formsnapshot;

import org.springframework.modulith.ApplicationModule;
