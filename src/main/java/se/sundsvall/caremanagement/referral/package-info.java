/**
 * Referral module — a referral/consultation sent on an errand to an external authority, and its response.
 *
 * <p>
 * Shared, type-agnostic and keyed by {@code errand_id}: an errand can be referred to one or more authorities for their
 * input. Captures the receiving {@code authority} (namespace-defined free text), {@code recipient}, {@code sentAt}/
 * {@code dueAt}, the {@code responseText} and a lifecycle {@code status} (SENT → RESPONDED).
 * </p>
 *
 * <p>
 * Accessed over REST (no cross-module Java dependency); depends only on the exposed {@code core} errand layer. Referral
 * rows are removed when their errand is deleted, via an {@code ErrandDeleted} listener.
 * </p>
 */
@ApplicationModule(displayName = "Referrals")
package se.sundsvall.caremanagement.referral;

import org.springframework.modulith.ApplicationModule;
