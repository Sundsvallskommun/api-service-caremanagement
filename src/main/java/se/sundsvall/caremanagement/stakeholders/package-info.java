/**
 * Stakeholders module — universal stakeholder data, per-type role registry.
 *
 * Type modules contribute role definitions via
 * {@link se.sundsvall.caremanagement.stakeholders.service.StakeholderRoleContribution} beans.
 */
@ApplicationModule(displayName = "Stakeholders")
package se.sundsvall.caremanagement.stakeholders;

import org.springframework.modulith.ApplicationModule;
