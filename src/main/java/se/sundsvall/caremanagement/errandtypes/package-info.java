/**
 * Errand types module — the public errand-type catalogue.
 *
 * <p>
 * Serves {@code /errand-types}: for each registered type slug it returns a form descriptor — display name,
 * application-type variant, allowed statuses, stakeholder roles and the per-type field specification — so a frontend
 * can
 * discover what (for example) a financial-assistance renewal model looks like without hard-coding it.
 * </p>
 *
 * <p>
 * Read-only and aggregating: type info comes from {@code core}'s
 * {@link se.sundsvall.caremanagement.core.service.registry.ErrandTypeRegistry},
 * roles from {@code stakeholders}, and the field specification from
 * {@link se.sundsvall.caremanagement.errandtypes.service.ErrandTypeSchemaContribution} beans that each type module
 * exposes. This module never depends on a type module — Spring wires the contributions in.
 * </p>
 */
@ApplicationModule(displayName = "Errand Types")
package se.sundsvall.caremanagement.errandtypes;

import org.springframework.modulith.ApplicationModule;
