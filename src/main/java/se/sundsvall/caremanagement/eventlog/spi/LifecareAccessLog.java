package se.sundsvall.caremanagement.eventlog.spi;

import java.util.List;

/**
 * The errand access log, for reads and writes careM makes in Lifecare ProfessionalWeb on an errand's behalf.
 *
 * <p>
 * The in-process twin of {@code POST .../errands/{errandId}/events/lifecare}: each entry becomes one row with source
 * LIFECARE, attributed to the caller identity of the current request (X-Sent-By), so the log answers who saw or wrote
 * what even though Lifecare itself shows the integration account.
 * </p>
 */
public interface LifecareAccessLog {

	/**
	 * Records accesses already made. Record after the Lifecare call, and nothing for a call Lifecare refused.
	 *
	 * @param municipalityId the municipality
	 * @param namespace      the namespace
	 * @param errandId       the errand the access was made for
	 * @param entries        the accesses
	 */
	void record(String municipalityId, String namespace, String errandId, List<LifecareAccessEntry> entries);
}
