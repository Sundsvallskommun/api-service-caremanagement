package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.util.List;
import java.util.Optional;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * An errand as its Lifecare calls see it: where it lives, and the Lifecare keys stored on it.
 *
 * @param municipalityId the municipality
 * @param namespace      the namespace
 * @param errandId       the errand
 * @param serviceId      the Lifecare insats id, null when it is not known yet
 * @param calculationId  the linked Lifecare calculation, null when none
 * @param decisionId     the linked Lifecare decision, null when none
 * @param paymentIds     the linked Lifecare payments, never null
 * @param periodYear     the year applied for, null when not set
 * @param periodMonth    the month applied for (1-12), null when not set
 */
public record LifecareErrand(String municipalityId, String namespace, String errandId, Integer serviceId, Integer calculationId, Integer decisionId,
	List<String> paymentIds, Integer periodYear, Integer periodMonth) {

	public LifecareErrand {
		paymentIds = Optional.ofNullable(paymentIds).map(List::copyOf).orElse(List.of());
	}

	/**
	 * The insats id, which every insats-scoped Lifecare call is keyed on.
	 *
	 * @return the insats id
	 */
	public int requireServiceId() {
		return Optional.ofNullable(serviceId)
			.orElseThrow(() -> Problem.valueOf(CONFLICT, "The errand has no Lifecare insats yet - it has to be opened in Lifecare first"));
	}

	public Optional<Integer> calculation() {
		return Optional.ofNullable(calculationId);
	}

	public Optional<Integer> decision() {
		return Optional.ofNullable(decisionId);
	}
}
