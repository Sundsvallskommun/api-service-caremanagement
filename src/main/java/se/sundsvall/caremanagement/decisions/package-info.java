/**
 * Decisions module — the errand's decision audit trail.
 *
 * Each {@code Decision} records a {@code decisionType} (e.g. RECOMMENDATION written by a process,
 * PAYMENT written by a handläggare), an optional {@code value}/{@code amount}/{@code decisionMessage}
 * and validity period ({@code decisionDate}/{@code periodFrom}/{@code periodTo}), and is owned by
 * exactly one errand. Read and written via {@code DecisionResource}.
 */
@ApplicationModule(displayName = "Decisions")
package se.sundsvall.caremanagement.decisions;

import org.springframework.modulith.ApplicationModule;
