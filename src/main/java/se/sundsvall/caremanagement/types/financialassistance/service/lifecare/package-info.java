/**
 * The errand's Lifecare ProfessionalWeb surface: careM's own connection to Lifecare, through which it reads and writes
 * on the caseworker's action in Draken. Draken's backend has no Lifecare connection; it forwards the caseworker's
 * actions to the errand's /lifecare routes.
 *
 * <p>
 * Every call derives its Lifecare keys from the errand (the insats id, the applicant, the linked calculation, decision
 * and payments) rather than taking them from the caller, logs itself in the errand's access log
 * ({@code LifecareAccessRecorder}), and links what it created back onto the errand in the same request.
 * </p>
 */
package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;
