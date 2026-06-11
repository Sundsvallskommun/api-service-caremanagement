/**
 * Decisions module — universal decision base.
 *
 * Provides the shared shape (id, errand_id, outcome, motivation, decided_by,
 * decided_at). Type modules can extend by owning their own
 * {@code errand_<type>_decision_detail} table with FK to {@code decision.id}.
 *
 * Implementation lands in Phase 2 (see docs/architecture/migration-plan.md).
 */
@ApplicationModule(displayName = "Decisions")
package se.sundsvall.caremanagement.decisions;

import org.springframework.modulith.ApplicationModule;
