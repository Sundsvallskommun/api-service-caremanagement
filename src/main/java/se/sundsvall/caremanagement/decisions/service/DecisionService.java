package se.sundsvall.caremanagement.decisions.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.core.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.core.integration.db.model.ErrandEntity;
import se.sundsvall.caremanagement.decisions.api.model.Decision;
import se.sundsvall.caremanagement.decisions.integration.db.DecisionRepository;
import se.sundsvall.caremanagement.decisions.integration.db.model.DecisionEntity;
import se.sundsvall.caremanagement.shared.NotificationRequest;
import se.sundsvall.dept44.problem.Problem;

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

	private final ErrandRepository errandRepository;
	private final DecisionRepository decisionRepository;
	private final ApplicationEventPublisher eventPublisher;

	DecisionService(final ErrandRepository errandRepository, final DecisionRepository decisionRepository, final ApplicationEventPublisher eventPublisher) {
		this.errandRepository = errandRepository;
		this.decisionRepository = decisionRepository;
		this.eventPublisher = eventPublisher;
	}

	public String create(final String municipalityId, final String namespace, final String errandId, final Decision decision) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		final var saved = decisionRepository.save(toDecisionEntity(decision, errandId));
		publishDecisionNotifications(municipalityId, namespace, errand, decision);
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public Decision read(final String municipalityId, final String namespace, final String errandId, final String decisionId) {
		return toDecision(findDecision(municipalityId, namespace, errandId, decisionId));
	}

	@Transactional(readOnly = true)
	public List<Decision> readAll(final String municipalityId, final String namespace, final String errandId) {
		ensureErrandExists(municipalityId, namespace, errandId);
		return toDecisionList(decisionRepository.findByErrandIdOrderByCreatedDesc(errandId));
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String decisionId) {
		final var entity = findDecision(municipalityId, namespace, errandId, decisionId);
		decisionRepository.delete(entity);
	}

	private ErrandEntity findErrand(final String municipalityId, final String namespace, final String errandId) {
		return errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private void ensureErrandExists(final String municipalityId, final String namespace, final String errandId) {
		findErrand(municipalityId, namespace, errandId);
	}

	private DecisionEntity findDecision(final String municipalityId, final String namespace, final String errandId, final String decisionId) {
		ensureErrandExists(municipalityId, namespace, errandId);
		return decisionRepository.findByErrandIdAndId(errandId, decisionId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, DECISION_NOT_FOUND_MESSAGE.formatted(decisionId, errandId, namespace, municipalityId)));
	}

	private void publishDecisionNotifications(final String municipalityId, final String namespace, final ErrandEntity errand, final Decision decision) {
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
		final var description = "Decision recorded: %s = %s".formatted(decision.getDecisionType(), decision.getValue());
		recipients.forEach(ownerId -> eventPublisher.publishEvent(new NotificationRequest(
			municipalityId, namespace, errand.getId(), ownerId, decision.getCreatedBy(), "CREATE", "DECISION", description)));
	}
}
