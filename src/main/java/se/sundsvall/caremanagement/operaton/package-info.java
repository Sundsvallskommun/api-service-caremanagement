/**
 * Operaton (BPMN) integration module. Generic glue around the Operaton REST client and the
 * {@code /process-message} correlation endpoint.
 *
 * Type modules kick off, correlate, and seed variables on their own BPMN processes via
 * {@link se.sundsvall.caremanagement.operaton.service.ProcessService} (D6: BPMN is per-type,
 * deploy-time — the envelope service does NOT start processes).
 *
 * Declared as a Modulith shared module on {@link se.sundsvall.caremanagement.Application
 * 
 * @Modulithic(sharedModules = "operaton")} so any module can depend on {@code ProcessService}
 *                           without listing it in {@code allowedDependencies}.
 */
@ApplicationModule(displayName = "Operaton")
package se.sundsvall.caremanagement.operaton;

import org.springframework.modulith.ApplicationModule;
