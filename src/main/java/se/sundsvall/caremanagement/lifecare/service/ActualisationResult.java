package se.sundsvall.caremanagement.lifecare.service;

/**
 * The outcome of creating a financial assistance intake in Lifecare FamilyCare: the created actualisation id and, when
 * a caseworker could be resolved off the applicant's most recent Service, the network (AD) user id to assign as the
 * careM errand's {@code assignedUserId}. The assignee is {@code null} when no caseworker could be resolved.
 *
 * @param actualisationId the id of the actualisation created in Lifecare FamilyCare
 * @param assignedUserId  the network/AD user id of the resolved caseworker, or {@code null} when none was resolved
 */
public record ActualisationResult(Integer actualisationId, String assignedUserId) {
}
