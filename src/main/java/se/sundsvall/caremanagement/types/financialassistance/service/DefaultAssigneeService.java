package se.sundsvall.caremanagement.types.financialassistance.service;

import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

import static java.util.Optional.ofNullable;

/**
 * Resolves the default caseworker for a financial assistance errand that arrived without an assignee — evaluates the
 * modeler-editable {@code Decision_defaultAssignee} DMN in the operaton engine (the same place the income raw list,
 * expense rules and the renewal delta rules live) and returns the configured assignee: the network/AD user id used as
 * the errand {@code assignedUserId}. This is the fallback for cases the Lifecare {@code CaseworkerResolver} can't place
 * (a new application has no prior intervention; a renewal whose previous caseworker can't be matched), giving the
 * business unit one
 * editable place to route incoming errands without a code change.
 *
 * <p>
 * The municipalityId is passed as a decision variable so the table <em>can</em> key on it later (e.g. one default per
 * municipality) without touching this service. Best-effort: when the decision is unavailable, missing, or returns
 * nothing, this resolves to {@link Optional#empty()} so errand creation is never blocked and the errand is simply left
 * unassigned. No personal data is logged here.
 */
@Service
public class DefaultAssigneeService {

	static final String DECISION_KEY = "Decision_defaultAssignee";
	static final String VAR_MUNICIPALITY_ID = "municipalityId";
	private static final String OUTPUT_ASSIGNED_USER_ID = "assignedUserId";

	private static final Logger LOG = LoggerFactory.getLogger(DefaultAssigneeService.class);

	private final ProcessService processService;

	DefaultAssigneeService(final ProcessService processService) {
		this.processService = processService;
	}

	/**
	 * Resolve the modeler-configured default assignee for the municipality.
	 *
	 * @param  municipalityId the municipality the errand belongs to
	 * @return                the default assignee (errand {@code assignedUserId}), or empty when none is configured
	 */
	public Optional<String> resolve(final String municipalityId) {
		try {
			final var rows = processService.evaluateDecision(municipalityId, DECISION_KEY, Map.of(VAR_MUNICIPALITY_ID, municipalityId));
			if (rows.isEmpty()) {
				return Optional.empty();
			}
			return ofNullable(rows.getFirst().get(OUTPUT_ASSIGNED_USER_ID))
				.map(Object::toString)
				.map(String::trim)
				.filter(StringUtils::hasText);
		} catch (final RuntimeException e) {
			LOG.warn("Default assignee rules ({}) unavailable — leaving errand unassigned", DECISION_KEY, e);
			return Optional.empty();
		}
	}
}
