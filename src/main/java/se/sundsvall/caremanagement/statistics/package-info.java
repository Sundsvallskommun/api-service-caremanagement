/**
 * Statistics module — read-only aggregated errand counts for the caseworker interface.
 *
 * Universal across all errand types: aggregates the core errand envelope in memory into counts per status, counts per
 * assigned user, and the number of unassigned errands, optionally scoped by errand type and creation date. Status
 * values are namespace-defined free text, so the aggregation is lifecycle-agnostic.
 */
@ApplicationModule(displayName = "Statistics")
package se.sundsvall.caremanagement.statistics;

import org.springframework.modulith.ApplicationModule;
