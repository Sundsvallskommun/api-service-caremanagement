/**
 * RPA (UiPath Orchestrator) module.
 *
 * <p>
 * The bridge between CareManagement and the parts of Lifecare that have no FamilyCare API — watches/reminders, journal,
 * documents, the
 * decision itself, registering the payment. CareManagement does not drive Lifecare directly; it drops a UiPath
 * <em>queue item</em> ({@code AddQueueItem}) referencing the errandId and a robot performs the GUI flow out of band.
 * Both directions are the same trigger: a <em>fetch</em> action (robot reads Lifecare → writes the supplements back via
 * the existing CareManagement endpoints) and <em>write</em> actions (robot types CareManagement-held data into
 * Lifecare).
 * </p>
 */
@ApplicationModule(displayName = "RPA")
package se.sundsvall.caremanagement.rpa;

import org.springframework.modulith.ApplicationModule;
