/**
 * Financial assistance type module.
 *
 * <p>
 * The financial assistance errand type. One {@code financial-assistance} errand carries a citizen's monthly application
 * for financial aid
 * (new application / renewal / supplementary application, discriminated by {@code applicationType}). The strongly-typed
 * application
 * data — household children, costs, incomes, pending benefits, assets, per-person planning and payment — lives on this
 * module's own {@code errand_financial_assistance*} tables, sharing the primary key with {@code errand.id}. No
 * parameters blob.
 * </p>
 *
 * <p>
 * Registers its {@code ErrandTypeContribution} (statuses + transitions) and {@code StakeholderRoleContribution}
 * (APPLICANT, CO_APPLICANT) at startup. Envelope creation and reads go through the exposed {@code core} service. On
 * errand deletion every {@code errand_financial_assistance*} table is removed by its {@code ON DELETE CASCADE} foreign
 * key to {@code errand} — the main row (with its child rows and element collections), the calculation draft (which in
 * turn cascades to its norm person/income/expense rows), and the monitoring, section-approval and warning satellites.
 * </p>
 */
@ApplicationModule(displayName = "Financial Assistance")
package se.sundsvall.caremanagement.types.financialassistance;

import org.springframework.modulith.ApplicationModule;
