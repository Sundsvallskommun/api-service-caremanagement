/**
 * Maintenance module — operational housekeeping jobs.
 *
 * Currently the (disabled-by-default, demo-only) nightly database reset. Enabled only via
 * {@code maintenance.database-cleanup.enabled=true} in throwaway demo environments; never active in
 * mainline/production.
 */
@ApplicationModule(displayName = "Maintenance")
package se.sundsvall.caremanagement.maintenance;

import org.springframework.modulith.ApplicationModule;
