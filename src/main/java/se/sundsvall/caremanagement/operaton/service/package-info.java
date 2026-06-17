/**
 * Operaton service layer.
 *
 * <p>
 * Exposed so type modules can kick off, correlate and seed variables on their own BPMN processes through
 * {@link se.sundsvall.caremanagement.operaton.service.ProcessService} (D6: BPMN is per-type — the envelope service does
 * not start processes).
 * </p>
 */
@NamedInterface("service")
package se.sundsvall.caremanagement.operaton.service;

import org.springframework.modulith.NamedInterface;
