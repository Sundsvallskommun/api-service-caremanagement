package se.sundsvall.caremanagement.decisions.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.core.api.model.Errand;
import se.sundsvall.caremanagement.core.spi.ErrandQueryService;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.api.model.DecisionLifecareResult;
import se.sundsvall.caremanagement.decisions.integration.db.DecisionRepository;
import se.sundsvall.caremanagement.decisions.integration.db.model.DecisionEntity;
import se.sundsvall.caremanagement.decisions.service.event.DecisionCreated;
import se.sundsvall.caremanagement.shared.ErrandAccessGuard;
import se.sundsvall.caremanagement.shared.NotificationRequest;
import se.sundsvall.dept44.problem.Problem;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.util.StringUtils.hasText;
import static se.sundsvall.caremanagement.decisions.service.mapper.DecisionMapper.toDecision;
import static se.sundsvall.caremanagement.decisions.service.mapper.DecisionMapper.toDecisionEntity;
import static se.sundsvall.caremanagement.decisions.service.mapper.DecisionMapper.toDecisionList;

@Service
@Transactional
public class DecisionService {

	private static final String ERRAND_NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String DECISION_NOT_FOUND_MESSAGE = "No decision with id '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";

	/** Set by finalize when it hands the decision over to be written into Lifecare. */
	public static final String LIFECARE_STATUS_PENDING = "PENDING";
	static final String LIFECARE_STATUS_SYNCED = "SYNCED";
	static final String LIFECARE_STATUS_FAILED = "FAILED";
	static final String OUTCOME_FAILED = "FAILED";
	static final String ERROR_DETAIL_REQUIRED = "detail is required when outcome is FAILED — it is Lifecare's own message, shown to the caseworker";
	static final String ERROR_ALREADY_SYNCED = "Decision is already SYNCED in Lifecare and cannot be reported as FAILED";

	private final ErrandQueryService errandQueryService;
	private final DecisionRepository decisionRepository;
	private final ApplicationEventPublisher publisher;
	private final ErrandAccessGuard errandGuard;

	DecisionService(final ErrandQueryService errandQueryService, final DecisionRepository decisionRepository, final ApplicationEventPublisher publisher, final ErrandAccessGuard errandGuard) {
		this.errandQueryService = errandQueryService;
		this.decisionRepository = decisionRepository;
		this.publisher = publisher;
		this.errandGuard = errandGuard;
	}

	public String create(final String municipalityId, final String namespace, final String errandId, final Decision decision) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		final var saved = decisionRepository.save(toDecisionEntity(decision, errandId));
		publisher.publishEvent(new DecisionCreated(saved.getId(), errandId, municipalityId, namespace,
			decision.getDecisionType(), decision.getValue(), decision.getCreatedBy(), now(systemDefault()).truncatedTo(MILLIS)));
		publishDecisionNotifications(municipalityId, namespace, errand, decision);
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public Decision read(final String municipalityId, final String namespace, final String errandId, final String decisionId) {
		return toDecision(findDecision(municipalityId, namespace, errandId, decisionId));
	}

	@Transactional(readOnly = true)
	public List<Decision> readAll(final String municipalityId, final String namespace, final String errandId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		return toDecisionList(decisionRepository.findByErrandIdOrderByCreatedDesc(errandId));
	}

	/**
	 * Record the report on writing the decision into Lifecare. {@code ALREADY_EXISTS} counts as success. Re-posting the
	 * same outcome is idempotent; reporting {@code FAILED} on a decision already {@code SYNCED} is a {@code 409}, because
	 * that would silently tell the caseworker a decision Lifecare has is missing.
	 */
	public Decision recordLifecareResult(final String municipalityId, final String namespace, final String errandId, final String decisionId,
		final DecisionLifecareResult result) {

		final var entity = findDecision(municipalityId, namespace, errandId, decisionId);

		if (OUTCOME_FAILED.equals(result.getOutcome())) {
			if (!hasText(result.getDetail())) {
				throw Problem.valueOf(BAD_REQUEST, ERROR_DETAIL_REQUIRED);
			}
			if (LIFECARE_STATUS_SYNCED.equals(entity.getLifecareStatus())) {
				throw Problem.valueOf(CONFLICT, ERROR_ALREADY_SYNCED);
			}
			return toDecision(decisionRepository.save(entity
				.withLifecareStatus(LIFECARE_STATUS_FAILED)
				.withLifecareDetail(result.getDetail())));
		}

		return toDecision(decisionRepository.save(entity
			.withLifecareStatus(LIFECARE_STATUS_SYNCED)
			.withLifecareId(ofNullable(result.getLifecareId()).filter(StringUtils::hasText).orElse(entity.getLifecareId()))
			.withLifecareDetail(null)));
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String decisionId) {
		final var entity = findDecision(municipalityId, namespace, errandId, decisionId);
		decisionRepository.delete(entity);
	}

	private Errand findErrand(final String municipalityId, final String namespace, final String errandId) {
		return errandQueryService.findErrand(municipalityId, namespace, errandId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private DecisionEntity findDecision(final String municipalityId, final String namespace, final String errandId, final String decisionId) {
		errandGuard.verifyExistingErrand(municipalityId, namespace, errandId);
		return decisionRepository.findByErrandIdAndId(errandId, decisionId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, DECISION_NOT_FOUND_MESSAGE.formatted(decisionId, errandId, namespace, municipalityId)));
	}

	private void publishDecisionNotifications(final String municipalityId, final String namespace, final Errand errand, final Decision decision) {
		final Set<String> recipients = new LinkedHashSet<>();
		if (hasText(errand.getReporterUserId())) {
			recipients.add(errand.getReporterUserId());
		}
		if (hasText(errand.getAssignedUserId())) {
			recipients.add(errand.getAssignedUserId());
		}
		if (recipients.isEmpty()) {
			return;
		}
		final var description = DecisionNotificationText.describe(decision.getDecisionType(), decision.getValue());
		recipients.forEach(ownerId -> publisher.publishEvent(new NotificationRequest(
			municipalityId, namespace, errand.getId(), ownerId, decision.getCreatedBy(), "CREATE", "DECISION", description)));
	}
}
