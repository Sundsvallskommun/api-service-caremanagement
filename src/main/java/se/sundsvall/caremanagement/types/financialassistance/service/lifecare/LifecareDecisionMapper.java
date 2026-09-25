package se.sundsvall.caremanagement.types.financialassistance.service.lifecare;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionReason;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionType;
import se.sundsvall.caremanagement.types.financialassistance.api.model.lifecare.LifecareDecisionView;
import tools.jackson.databind.JsonNode;

import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.OUTCOME_AVSLAG;
import static se.sundsvall.caremanagement.types.financialassistance.configuration.FinancialAssistanceModuleConfig.OUTCOME_BIFALL;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJson.decimal;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJson.elements;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJson.integer;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJson.isTrue;
import static se.sundsvall.caremanagement.types.financialassistance.service.lifecare.LifecareJson.text;

/**
 * Lifecare's beslut objects as careM serves them, and careM's outcome for a Lifecare beslutstyp.
 */
final class LifecareDecisionMapper {

	/**
	 * careM's outcome for each Lifecare beslutstyp category careM registers. Every bifall type (category 0) and every
	 * avslag type (category 10) the insats offers can be saved. Other categories (återkrav, förskott and so on) are
	 * listed but refused, since what else they set in motion in Lifecare is not captured.
	 *
	 * <p>
	 * TODO: Lifecare's beslutstyper are not fully configured for ekonomiskt bistånd yet. Revisit this mapping, and which
	 * categories careM lets through, once they are.
	 * </p>
	 */
	static final Map<Integer, String> OUTCOME_BY_CATEGORY = Map.of(0, OUTCOME_BIFALL, 10, OUTCOME_AVSLAG);

	private LifecareDecisionMapper() {}

	/**
	 * careM's outcome for a Lifecare beslutstyp category.
	 *
	 * @param  category the category node
	 * @return          the outcome, empty for a category careM does not register
	 */
	static Optional<String> outcomeFor(final JsonNode category) {
		return integer(category).map(OUTCOME_BY_CATEGORY::get);
	}

	/**
	 * The registered beslut, cleaned up for the Beslut tab and finalize. The personnummer is left behind.
	 *
	 * @param  saved the beslut as GetDecision returns it
	 * @return       the view
	 */
	static LifecareDecisionView toView(final JsonNode saved) {
		return new LifecareDecisionView(
			integer(saved.path("decisionId")).orElse(null),
			integer(saved.path("decisionCode")).orElse(null),
			outcomeFor(saved.path("decisionType")).orElse(null),
			text(saved.path("date")).orElse(""),
			text(saved.path("fromDate")).orElse(null),
			text(saved.path("toDate")).orElse(null),
			decimal(saved.path("amount")).orElse(BigDecimal.ZERO),
			// Lifecare sends 0 (or an empty string) for no orsak.
			integer(saved.path("reasonCode")).filter(code -> code > 0).orElse(null),
			text(saved.path("reason")).orElse(null),
			text(saved.path("message")).orElse(null),
			isTrue(saved.path("lockedMessage")),
			text(saved.path("decisionMakerName")).or(() -> text(saved.path("decisionMaker"))).orElse(""));
	}

	/**
	 * The active beslutstyper the insats offers, in Lifecare's order.
	 *
	 * @param  proposal the underlag, GetProposalForService
	 * @return          the beslutstyper
	 */
	static List<LifecareDecisionType> toTypes(final JsonNode proposal) {
		return elements(proposal.path("decisionTypes")).stream()
			.filter(type -> isTrue(type.path("isActive")))
			.map(type -> new LifecareDecisionType(
				integer(type.path("code")).orElse(null),
				text(type.path("name")).orElse(null),
				outcomeFor(type.path("type")).orElse(null),
				isTrue(type.path("requiresFromDate")),
				isTrue(type.path("requiresToDate"))))
			.toList();
	}

	/**
	 * The choosable orsaker in Lifecare's catalogue (its leaves), each with the heading it sits under.
	 *
	 * @param  catalogue the catalogue, GetMappedDecisionReasons
	 * @return           the orsaker
	 */
	static List<LifecareDecisionReason> toReasons(final JsonNode catalogue) {
		return toReasons(catalogue, "").toList();
	}

	private static Stream<LifecareDecisionReason> toReasons(final JsonNode catalogue, final String header) {
		return elements(catalogue).stream().flatMap(node -> {
			final var nodeHeader = text(node.path("header")).orElse(header);
			final var code = integer(node.path("reasonCode"));
			final var own = code.map(value -> new LifecareDecisionReason(value, text(node.path("name")).orElse(null), nodeHeader)).stream();
			final var childHeader = code.map(_ -> nodeHeader).orElseGet(() -> text(node.path("name")).orElse(""));
			return Stream.concat(own, toReasons(node.path("options"), childHeader));
		});
	}
}
