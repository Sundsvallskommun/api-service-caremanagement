/**
 * Status history module — listens to {@code ErrandStatusChanged} and persists each transition.
 *
 * Pure subscriber: no public API for writing entries — every transition is recorded via the event listener. Reads are
 * exposed via {@code StatusHistoryResource}.
 */
@ApplicationModule(displayName = "Status History")
package se.sundsvall.caremanagement.statushistory;

import org.springframework.modulith.ApplicationModule;
