package se.sundsvall.caremanagement.shared;

/**
 * Cross-cutting guard for errand access. Sub-resource modules (notes, journal, document, conversation,
 * notifications, permits, referrals, status history, RPA, process-messages, …) depend on this thin port
 * instead of pulling in the whole {@code core} {@code ErrandService} just to check that the parent errand
 * exists in the caller's tenant. The {@code core} module provides the implementation.
 *
 * <p>
 * This is the existence/authorization seam (mirrors the {@code accessControlService} pattern in
 * api-service-support-management): today it asserts existence; a future overload can fold in the
 * caller's read/write authorization on the same call.
 */
public interface ErrandAccessGuard {

	/**
	 * Asserts that an errand envelope exists for the given tenant. Throws a {@code NOT_FOUND} problem when no such
	 * errand exists in the {@code (municipalityId, namespace)}; returns nothing when it does. The errand-scoped child
	 * query (e.g. {@code findByIdAndErrandId}) that actually closes cross-tenant IDOR still lives in each module's own
	 * repository — this guard only covers the parent-errand existence check.
	 *
	 * @param municipalityId the owning municipality
	 * @param namespace      the owning namespace
	 * @param errandId       the errand to assert
	 */
	void verifyExistingErrand(String municipalityId, String namespace, String errandId);
}
