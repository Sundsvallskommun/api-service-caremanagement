/**
 * Event log module — records who/what/when for every errand-scoped HTTP request, both reads and writes.
 *
 * Pure access/activity log: a {@code HandlerInterceptor} records one row per errand-scoped request, capturing the
 * dept44 {@code Identifier} (the {@code X-Sent-By} actor) on the request thread — the only place it is reliably
 * available, since Modulith {@code @ApplicationModuleListener}s run on a different thread after commit. The log is
 * independent of the errand lifecycle (no foreign key) so it can record deletions and survive errand removal. Reads are
 * exposed via {@code ErrandEventResource}.
 */
@ApplicationModule(displayName = "Event Log")
package se.sundsvall.caremanagement.eventlog;

import org.springframework.modulith.ApplicationModule;
