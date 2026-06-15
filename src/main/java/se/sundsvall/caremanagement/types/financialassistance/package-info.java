/**
 * Financial assistance (ekonomiskt bistånd) type module.
 *
 * <p>
 * The EB errand type. One {@code financial-assistance} errand carries a citizen's monthly application for financial aid
 * (nyansökan / återansökan / tilläggsansökan, discriminated by {@code applicationType}). The strongly-typed application
 * data — household children, costs, incomes, pending benefits, assets, per-person planning and payment — lives on this
 * module's own {@code errand_financial_assistance*} tables, sharing the primary key with {@code errand.id}. No
 * parameters blob.
 * </p>
 *
 * <p>
 * Registers its {@code ErrandTypeContribution} (statuses + transitions) and {@code StakeholderRoleContribution}
 * (APPLICANT, CO_APPLICANT) at startup. Envelope creation and reads go through the exposed {@code core} service; rows
 * are removed when the errand is deleted via an {@code ErrandDeleted} listener.
 * </p>
 */
@ApplicationModule(displayName = "Financial Assistance")
package se.sundsvall.caremanagement.types.financialassistance;

import org.springframework.modulith.ApplicationModule;
