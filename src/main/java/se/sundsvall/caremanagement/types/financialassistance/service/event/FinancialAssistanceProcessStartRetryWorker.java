package se.sundsvall.caremanagement.types.financialassistance.service.event;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.service.event.ErrandCreated;
import se.sundsvall.caremanagement.core.spi.ErrandQueryService;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.SLUGS;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.STATUS_RECEIVED;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

/**
 * Starts the process of a financial assistance errand whose start at creation did not take. The start
 * ({@link FinancialAssistanceErrandCreatedListener}) is best-effort — an engine that is down, a token request that
 * fails or a call that times out is logged and given up on — and without a process an errand stays {@code RECEIVED}
 * for good: no preparation, no actualisation, and nobody is told.
 *
 * <p>
 * An errand is a candidate when it is a financial assistance errand in status exactly {@code RECEIVED}, has no process
 * instance linked, and was created between {@code maxAge} and {@code minAge} ago. The lower bound keeps the job clear
 * of the start that follows the creation; the upper one is what makes the retrying finite, and keeps old rows from
 * being woken up. Any other status is never selected — an errand frozen as {@code NEEDS_MANUAL_REVIEW} in particular
 * stays put until a caseworker releases it ({@link FinancialAssistanceReleaseListener}).
 *
 * <p>
 * For each candidate:
 * <ol>
 * <li>Ask the engine whether the errand already has a running instance of its process (businessKey = errandId), once
 * per process for the whole batch ({@link ProcessService#activeBusinessKeys}). If it does, the start did take and only
 * the write back to the errand failed: nothing is started and nothing is changed. If the engine cannot answer, none of
 * that process's errands is touched — an unanswered question is not "no instance", and starting blind is how a second
 * instance gets made.</li>
 * <li>Run the classification the creation runs ({@link FinancialAssistanceErrandCreatedProcessor#assignAndClassify}) —
 * default caseworker, protected identity, recently closed. An errand whose start failed may equally be one whose
 * classification never completed, and the protected-identity check is the hard stop before automatic preparation reads
 * SSBTEK and writes to Lifecare: a retry must not be a way around it. An errand a guard holds is frozen for manual
 * review, exactly as it would have been at creation.</li>
 * <li>Start the process through {@link FinancialAssistanceErrandCreatedProcessor#startProcess}, i.e. the same
 * {@link FinancialAssistanceProcessStarter} path as the normal start, so the start variables are identical.</li>
 * </ol>
 *
 * <p>
 * Every errand is handled on its own: a failure is logged as a warning (errand id only, never anything from the
 * application) and the rest of the batch still runs. An errand that is still unstarted when it passes {@code maxAge}
 * is logged once, as an error, and left alone — it needs someone. "Once" holds for as long as this node runs; the
 * report only looks {@value #AGED_OUT_REPORT_WINDOW_HOURS} hours past {@code maxAge}, so a restart repeats it at most
 * for that stretch and never digs up old rows.
 */
@Component
@EnableConfigurationProperties(FinancialAssistanceProcessStartRetryProperties.class)
class FinancialAssistanceProcessStartRetryWorker {

	/** How far past {@code maxAge} an unstarted errand is still reported as aged out. */
	static final long AGED_OUT_REPORT_WINDOW_HOURS = 24;

	private static final Logger LOG = LoggerFactory.getLogger(FinancialAssistanceProcessStartRetryWorker.class);

	/**
	 * What a run found and did.
	 *
	 * @param candidates     errands still received without a process and young enough to retry
	 * @param started        of those, the ones whose process is now started and linked
	 * @param alreadyRunning of those, the ones the engine already runs a process for — left as they are
	 * @param held           of those, the ones a guard froze for manual review instead of starting
	 * @param failed         of those, the ones that could not be started, or whose engine could not be asked
	 * @param agedOut        errands unstarted for longer than {@code maxAge}, reported and not retried
	 */
	record Result(int candidates, int started, int alreadyRunning, int held, int failed, int agedOut) {}

	private enum Attempt {
		STARTED, ALREADY_RUNNING, HELD, FAILED, SKIPPED
	}

	private final ErrandQueryService errandQueryService;
	private final ProcessService processService;
	private final FinancialAssistanceErrandCreatedProcessor processor;
	private final FinancialAssistanceProcessStartRetryProperties properties;

	/** Errands already reported as aged out by this node, so each is reported once per run of the node. */
	private final Set<String> reportedAgedOut = ConcurrentHashMap.newKeySet();

	FinancialAssistanceProcessStartRetryWorker(final ErrandQueryService errandQueryService, final ProcessService processService,
		final FinancialAssistanceErrandCreatedProcessor processor, final FinancialAssistanceProcessStartRetryProperties properties) {
		this.errandQueryService = errandQueryService;
		this.processService = processService;
		this.processor = processor;
		this.properties = properties;
	}

	Result retryUnstarted() {
		if (!properties.enabled()) {
			return new Result(0, 0, 0, 0, 0, 0);
		}

		final var now = OffsetDateTime.now(ZoneId.systemDefault());
		final var retryFrom = now.minus(properties.maxAge());
		final var unstarted = errandQueryService.findWithoutProcessInstance(properties.municipalityId(), properties.namespace(), SLUGS,
			STATUS_RECEIVED, retryFrom.minusHours(AGED_OUT_REPORT_WINDOW_HOURS), now.minus(properties.minAge()));

		final var agedOut = unstarted.stream().filter(errand -> isCreatedBefore(errand, retryFrom)).toList();
		final var retryable = unstarted.stream().filter(errand -> !isCreatedBefore(errand, retryFrom)).toList();

		reportAgedOut(agedOut);

		final var tally = new EnumMap<Attempt, Integer>(Attempt.class);
		byProcess(retryable).forEach((processDefinitionName, errands) -> retryProcess(processDefinitionName, errands, tally));

		return new Result(retryable.size(), count(tally, Attempt.STARTED), count(tally, Attempt.ALREADY_RUNNING), count(tally, Attempt.HELD),
			count(tally, Attempt.FAILED), agedOut.size());
	}

	/** The candidates grouped by the process their type starts, so the engine is asked once per process. */
	private static Map<String, List<Errand>> byProcess(final List<Errand> errands) {
		final var byProcess = new LinkedHashMap<String, List<Errand>>();
		errands.forEach(errand -> FinancialAssistanceProcessStarter.processDefinitionName(errand.getTypeSlug())
			.ifPresent(processDefinitionName -> byProcess.computeIfAbsent(processDefinitionName, _ -> new ArrayList<>()).add(errand)));
		return byProcess;
	}

	private void retryProcess(final String processDefinitionName, final List<Errand> errands, final EnumMap<Attempt, Integer> tally) {
		final Set<String> running;
		try {
			running = processService.activeBusinessKeys(properties.municipalityId(), processDefinitionName);
		} catch (final RuntimeException e) {
			// Not knowing is not the same as none running: start nothing for this process, and try again next run.
			LOG.warn("Could not ask the engine which '{}' instances are running ({}); leaving {} errand(s) without a process for the next run",
				sanitizeForLogging(processDefinitionName), e.getClass().getSimpleName(), errands.size());
			tally.merge(Attempt.FAILED, errands.size(), Integer::sum);
			return;
		}
		errands.forEach(errand -> tally.merge(retryOne(errand, processDefinitionName, running), 1, Integer::sum));
	}

	/** One errand, its failures kept to it so the rest of the batch still runs. */
	private Attempt retryOne(final Errand errand, final String processDefinitionName, final Set<String> running) {
		final var errandId = errand.getId();
		if (running.contains(errandId)) {
			LOG.info("Errand {} already has a running '{}'; not starting another", sanitizeForLogging(errandId), sanitizeForLogging(processDefinitionName));
			return Attempt.ALREADY_RUNNING;
		}
		try {
			// The batch was read some time ago; a caseworker or the process itself may have moved the errand on since.
			if (current(errand).filter(this::isUnstarted).isEmpty()) {
				return Attempt.SKIPPED;
			}
			final var event = new ErrandCreated(errandId, errand.getTypeSlug(), errand.getMunicipalityId(), errand.getNamespace(),
				errand.getReporterUserId(), errand.getAssignedUserId(), errand.getCreated());
			return switch (processor.assignAndClassify(event)) {
				case NOT_FINANCIAL_ASSISTANCE -> {
					LOG.info("Errand {} carries no financial assistance data; nothing to start", sanitizeForLogging(errandId));
					yield Attempt.SKIPPED;
				}
				case FROZEN -> {
					LOG.info("Errand {} held for manual review instead of a process start", sanitizeForLogging(errandId));
					yield Attempt.HELD;
				}
				case PROCEED -> start(event, processDefinitionName);
			};
		} catch (final RuntimeException e) {
			// The exception type only: what a lookup or the engine echoes back may concern the applicant.
			LOG.warn("Retrying the start of '{}' for errand {} failed ({}); the next run tries again until the errand is {} old",
				sanitizeForLogging(processDefinitionName), sanitizeForLogging(errandId), e.getClass().getSimpleName(), properties.maxAge());
			return Attempt.FAILED;
		}
	}

	private Attempt start(final ErrandCreated event, final String processDefinitionName) {
		// The starter keeps its failures to itself (it is best-effort at creation), so whether the start took is read off
		// the errand: a started process is linked to it.
		processor.startProcess(event);
		if (current(event).filter(errand -> hasText(errand.getProcessInstanceId())).isPresent()) {
			LOG.info("Started '{}' for errand {} on retry", sanitizeForLogging(processDefinitionName), sanitizeForLogging(event.errandId()));
			return Attempt.STARTED;
		}
		LOG.warn("The retried start of '{}' for errand {} did not take; the next run tries again until the errand is {} old",
			sanitizeForLogging(processDefinitionName), sanitizeForLogging(event.errandId()), properties.maxAge());
		return Attempt.FAILED;
	}

	/** One error per errand that has run out of retries — it is what tells someone the errand needs a hand. */
	private void reportAgedOut(final List<Errand> agedOut) {
		// Only ever remember what is still in the window, so the memory follows the window instead of growing.
		reportedAgedOut.retainAll(agedOut.stream().map(Errand::getId).collect(Collectors.toSet()));
		agedOut.stream()
			.filter(errand -> reportedAgedOut.add(errand.getId()))
			.forEach(errand -> LOG.error("Errand {} is still received without a process more than {} after it was created; it is no longer retried and needs its process started by hand",
				sanitizeForLogging(errand.getId()), properties.maxAge()));
	}

	private Optional<Errand> current(final Errand errand) {
		return errandQueryService.findErrand(errand.getMunicipalityId(), errand.getNamespace(), errand.getId());
	}

	private Optional<Errand> current(final ErrandCreated event) {
		return errandQueryService.findErrand(event.municipalityId(), event.namespace(), event.errandId());
	}

	private boolean isUnstarted(final Errand errand) {
		return STATUS_RECEIVED.equals(errand.getStatus()) && !hasText(errand.getProcessInstanceId());
	}

	private static boolean isCreatedBefore(final Errand errand, final OffsetDateTime cutoff) {
		return ofNullable(errand.getCreated()).map(created -> created.isBefore(cutoff)).orElse(false);
	}

	private static int count(final Map<Attempt, Integer> tally, final Attempt attempt) {
		return tally.getOrDefault(attempt, 0);
	}
}
