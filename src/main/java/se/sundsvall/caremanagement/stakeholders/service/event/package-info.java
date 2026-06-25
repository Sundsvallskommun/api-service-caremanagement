/**
 * Cross-module events published by the stakeholders module. Exposed as a named interface so other modules (e.g.
 * financial-assistance) may subscribe — mirrors {@code core.service.event}.
 */
@NamedInterface("events")
package se.sundsvall.caremanagement.stakeholders.service.event;

import org.springframework.modulith.NamedInterface;
