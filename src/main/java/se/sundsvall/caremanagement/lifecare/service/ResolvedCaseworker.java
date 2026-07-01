package se.sundsvall.caremanagement.lifecare.service;

/**
 * The caseworker resolved for a financial assistance intake: the Lifecare FC user id used as the actualisation
 * {@code CaseworkerId},
 * the network (AD) user id used as the careM errand {@code assignedUserId}, and the display name it was resolved from
 * (the Service's {@code Caseworker}). All three originate from the same FC {@code User}.
 *
 * @param caseworkerId   the FC user id (actualisation {@code CaseworkerId})
 * @param assignedUserId the network/AD user id (careM errand {@code assignedUserId})
 * @param fullName       the caseworker display name read from the person's most recent Service
 */
public record ResolvedCaseworker(String caseworkerId, String assignedUserId, String fullName) {
}
