package se.sundsvall.caremanagement.rpa.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.rpa.integration.RpaClient;
import se.sundsvall.caremanagement.rpa.integration.configuration.RpaProperties;
import se.sundsvall.caremanagement.rpa.integration.model.AddQueueItemParameters;
import se.sundsvall.caremanagement.rpa.integration.model.QueueItemData;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.CONFLICT;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Enqueues financial assistance RPA tasks on the UiPath Orchestrator. Each call adds one queue item to the configured
 * financial assistance queue, keyed by
 * {@code reference = errandId:action}; a robot then performs the matching Lifecare GUI flow out of band.
 *
 * <p>
 * The enqueue is idempotent on the Orchestrator side: re-running the financial assistance process (the daily loop, a
 * retried external
 * task) re-adds the same {@code (queue, reference)} item, which UiPath rejects with {@code 409}/{@code 1016} — that is
 * swallowed as success. The reference includes the {@code action} so the dedup is per-(errand, action): two
 * <em>different</em> actions on the same errand (e.g. fetch then write) are distinct items and neither shadows the
 * other. A Lifecare/Orchestrator outage is <strong>not</strong> swallowed; it surfaces so the caller (a topic worker)
 * lets the engine retry. Triggering is best-effort decoration on top of the real write, so callers that must not fail
 * on RPA should guard the call themselves.
 */
@Service
public class RpaService {

	private static final Logger LOG = LoggerFactory.getLogger(RpaService.class);

	private static final String NORMAL_PRIORITY = "Normal";
	private static final String DUPLICATE_CODE = "1016";
	private static final String KEY_ACTION = "action";
	private static final String KEY_ERRAND_ID = "errandId";
	private static final String KEY_MUNICIPALITY_ID = "municipalityId";
	private static final String KEY_NAMESPACE = "namespace";

	private final RpaClient rpaClient;
	private final RpaProperties properties;
	private final ErrandAccessGuard errandGuard;

	public RpaService(final RpaClient rpaClient, final RpaProperties properties, final ErrandAccessGuard errandGuard) {
		this.rpaClient = rpaClient;
		this.properties = properties;
		this.errandGuard = errandGuard;
	}

	/**
	 * Enqueue an RPA task for an errand.
	 *
	 * @param municipalityId the municipality whose Orchestrator folder the item is added to
	 * @param errandId       the errand the robot acts on (becomes the queue item reference)
	 * @param action         the {@link RpaAction} to enqueue
	 */
	public void enqueue(final String municipalityId, final String errandId, final RpaAction action) {
		enqueue(municipalityId, null, errandId, action, Map.of());
	}

	/**
	 * Enqueue an RPA task for an errand with extra hints for the robot.
	 *
	 * @param municipalityId  the municipality whose Orchestrator folder the item is added to
	 * @param namespace       the namespace the errand lives in — placed in the queue item's {@code SpecificContent} so the
	 *                        robot can reconstruct the namespace-scoped errand API path to write back (may be {@code null})
	 * @param errandId        the errand the robot acts on (becomes the queue item reference)
	 * @param action          the {@link RpaAction} to enqueue
	 * @param specificContent additional key/values placed in the queue item's {@code SpecificContent}
	 */
	public void enqueue(final String municipalityId, final String namespace, final String errandId, final RpaAction action, final Map<String, String> specificContent) {
		// Inbound enqueues (via RpaResource) carry a namespace — assert the errand exists in that tenant so a caller cannot
		// enqueue a robot job against a foreign/unknown errandId. Internal callers pass a null namespace and skip the check
		// (they act on an errand already resolved in the current flow).
		ofNullable(namespace).ifPresent(value -> errandGuard.verifyExistingErrand(municipalityId, value, errandId));

		// The action travels to the robot as its constant name — the wire value the SpecificContent and reference carry.
		final var actionName = action.name();

		if (!properties.enabled()) {
			LOG.info("RPA disabled — skipping {} for errand {}", sanitizeForLogging(actionName), sanitizeForLogging(errandId));
			return;
		}

		final var folderId = ofNullable(properties.folderIds().get(municipalityId))
			.orElseThrow(() -> new IllegalStateException("No RPA folder id configured for municipality " + municipalityId));

		final var content = new HashMap<String, String>(ofNullable(specificContent).orElse(Map.of()));
		content.put(KEY_ACTION, actionName);
		content.put(KEY_ERRAND_ID, errandId);
		content.put(KEY_MUNICIPALITY_ID, municipalityId);
		ofNullable(namespace).ifPresent(value -> content.put(KEY_NAMESPACE, value));

		// Reference is per-(namespace, errand, action) so the Orchestrator's unique-reference dedup only collapses re-runs
		// of the same action — distinct actions on the same errand remain separate queue items, and the same action on the
		// same errandId in different namespaces stays distinct too.
		final var reference = ofNullable(namespace).map(value -> value + ":").orElse("") + errandId + ":" + actionName;
		final var item = new AddQueueItemParameters(new QueueItemData(properties.queue(), reference, NORMAL_PRIORITY, content));

		try {
			rpaClient.addQueueItem(folderId, item);
			LOG.info("Enqueued RPA task {} for errand {}", sanitizeForLogging(actionName), sanitizeForLogging(errandId));
		} catch (final ThrowableProblem e) {
			if (isDuplicate(e)) {
				LOG.info("RPA task {} for errand {} already queued — skipping", sanitizeForLogging(actionName), sanitizeForLogging(errandId));
				return;
			}
			throw e;
		}
	}

	private boolean isDuplicate(final ThrowableProblem e) {
		return CONFLICT.equals(e.getStatus()) || Optional.ofNullable(e.getDetail()).orElse("").contains(DUPLICATE_CODE);
	}
}
