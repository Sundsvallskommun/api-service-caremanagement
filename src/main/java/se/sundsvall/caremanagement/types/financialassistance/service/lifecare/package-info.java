/**
 * The errand's Lifecare ProfessionalWeb surface: what Draken's caseworker reads and writes in Lifecare, done by careM
 * on the errand's behalf.
 *
 * <p>
 * Every call derives its Lifecare keys from the errand (the insats id, the applicant, the linked calculation, decision
 * and payments) rather than taking them from the caller, logs itself in the errand's access log, and links what it
 * created back onto the errand in the same request.
 * </p>
 */
package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;
