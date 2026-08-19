/**
 * Permit module — an issued permit with a validity period, conditions and a lifecycle status.
 *
 * <p>
 * Shared, type-agnostic and keyed by {@code errand_id} (like {@code decisions}): a decision on an errand can issue a
 * permit. Captures what the flat {@code Decision} cannot — {@code validFrom}/{@code validUntil} (validity period),
 * {@code conditions} and a lifecycle {@code status} (ACTIVE → REVOKED on revocation). {@code permitType} is
 * namespace-defined free text, so the module carries no domain-specific validity rules.
 * </p>
 *
 * <p>
 * Accessed over REST (no cross-module Java dependency); depends only on the exposed {@code core} errand layer, so it
 * stays inside its module boundary. Permit rows are removed when their errand is deleted, via an
 * {@code ErrandDeleted} listener.
 * </p>
 */
@ApplicationModule(displayName = "Permits")
package se.sundsvall.caremanagement.permit;

import org.springframework.modulith.ApplicationModule;
