package se.sundsvall.caremanagement.core.spi;

/**
 * Minimal read-model projection of an errand for statistics aggregation: the namespace-defined free-text
 * {@code status} and the {@code assignedUserId} (blank/null = unassigned). Materialised inside core so the statistics
 * module aggregates over this view instead of the JPA entity.
 */
public record ErrandStatusView(String status, String assignedUserId) {
}
