/**
 * Co-caseworkers module — lets a handläggare add or remove co-caseworkers (medhandläggare) on an errand, in
 * addition to the errand's single {@code assignedUserId}. Administered from Draken (see the sprint decision
 * doc {@code backlog/svar-verksamheten-2026-09-23.md} section 4 "Medhandläggare och notiser") — Lifecare's own
 * {@code coCaseworker} field is explicitly NOT the administration path.
 *
 * <p>
 * The {@code notifications} module widens its "notifications visible to a recipient" queries
 * ({@code NotificationRepository}, {@code NotificationErrandFilter}) to also match errands where the recipient
 * is a co-caseworker here — a notification stays a single row with a single {@code handled}/{@code acknowledged}
 * state shared by every recipient who can see it (the errand's assignee and its co-caseworkers alike). This
 * module never fans out or duplicates notification rows; it only supplies the extra visibility join.
 */
@ApplicationModule(displayName = "Co-caseworkers")
package se.sundsvall.caremanagement.cocaseworkers;

import org.springframework.modulith.ApplicationModule;
