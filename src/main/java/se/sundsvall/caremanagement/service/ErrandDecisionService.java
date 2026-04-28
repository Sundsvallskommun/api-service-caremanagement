package se.sundsvall.caremanagement.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.caremanagement.api.model.Decision;
import se.sundsvall.caremanagement.integration.db.DecisionRepository;
import se.sundsvall.caremanagement.integration.db.ErrandRepository;
import se.sundsvall.caremanagement.integration.db.model.DecisionEntity;
import se.sundsvall.caremanagement.integration.db.model.ErrandEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.caremanagement.service.mapper.DecisionMapper.toDecision;
import static se.sundsvall.caremanagement.service.mapper.DecisionMapper.toDecisionEntity;
import static se.sundsvall.caremanagement.service.mapper.DecisionMapper.toDecisionList;

@Service
@Transactional
public class ErrandDecisionService {

	private static final String ERRAND_NOT_FOUND_MESSAGE = "No errand with id '%s' found in namespace '%s' for municipality id '%s'";
	private static final String DECISION_NOT_FOUND_MESSAGE = "No decision with id '%s' found on errand '%s' in namespace '%s' for municipality id '%s'";

	private final ErrandRepository errandRepository;
	private final DecisionRepository decisionRepository;

	ErrandDecisionService(final ErrandRepository errandRepository, final DecisionRepository decisionRepository) {
		this.errandRepository = errandRepository;
		this.decisionRepository = decisionRepository;
	}

	public String create(final String municipalityId, final String namespace, final String errandId, final Decision decision) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		final var saved = decisionRepository.save(toDecisionEntity(decision, errand));
		return saved.getId();
	}

	@Transactional(readOnly = true)
	public Decision read(final String municipalityId, final String namespace, final String errandId, final String decisionId) {
		return toDecision(findDecision(municipalityId, namespace, errandId, decisionId));
	}

	@Transactional(readOnly = true)
	public List<Decision> readAll(final String municipalityId, final String namespace, final String errandId) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		return toDecisionList(errand.getDecisions());
	}

	public void delete(final String municipalityId, final String namespace, final String errandId, final String decisionId) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		final var decision = errand.getDecisions().stream()
			.filter(entity -> entity.getId().equals(decisionId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, DECISION_NOT_FOUND_MESSAGE.formatted(decisionId, errandId, namespace, municipalityId)));
		errand.getDecisions().remove(decision);
		errandRepository.save(errand);
	}

	private ErrandEntity findErrand(final String municipalityId, final String namespace, final String errandId) {
		return errandRepository.findByIdAndNamespaceAndMunicipalityId(errandId, namespace, municipalityId)
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, ERRAND_NOT_FOUND_MESSAGE.formatted(errandId, namespace, municipalityId)));
	}

	private DecisionEntity findDecision(final String municipalityId, final String namespace, final String errandId, final String decisionId) {
		final var errand = findErrand(municipalityId, namespace, errandId);
		return errand.getDecisions().stream()
			.filter(entity -> entity.getId().equals(decisionId))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, DECISION_NOT_FOUND_MESSAGE.formatted(decisionId, errandId, namespace, municipalityId)));
	}
}
