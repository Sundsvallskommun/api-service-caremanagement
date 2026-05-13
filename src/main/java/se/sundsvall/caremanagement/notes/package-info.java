/**
 * Notes module — free-form notes attached to an errand. Universal across all errand types.
 *
 * No relation to the parameter swamp — notes are just {@code (errandId, body, author, createdAt)}.
 */
@ApplicationModule(displayName = "Notes")
package se.sundsvall.caremanagement.notes;

import org.springframework.modulith.ApplicationModule;
