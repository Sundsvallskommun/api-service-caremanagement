package se.sundsvall.caremanagement.types.financialassistance.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.caremanagement.operaton.service.ProcessService;

import static java.util.Optional.ofNullable;

/**
 * The återansökan rule tables — the verksamhet's "Regelverk Drakel – återansökan" encoded as the modeler-editable
 * {@code rakel-eb-ateransokan} DMN deployment in the operaton engine. Three tables, one public method each; the
 * division of labour is the same as for the other EB decisions ({@link ExpenseRulesService},
 * {@link RenewalDeltaService}): the table <em>judges</em>, the caller gathers the input, iterates (per child, per
 * income, per planning) and fills the placeholders in the warning text.
 *
 * <ul>
 * <li>{@code Decision_ansokanFragor} — a warning that follows directly from one answer in the application
 * ({@link #question})</li>
 * <li>{@code Decision_inkomstMotForegaende} — an income type in the previous normberäkning against the same type in
 * the application ({@link #incomeAgainstPrevious})</li>
 * <li>{@code Decision_ansokanMotBerakning} — children / household count / norm against the previous normberäkning
 * ({@link #againstPreviousCalculation})</li>
 * </ul>
 *
 * <p>
 * The fourth table in the deployment, {@code Decision_barnBoendeDagar}, has no caller: the previous FamilyCare
 * calculation exposes no per-child day count, so there is nothing to compare the application's days against.
 * </p>
 *
 * <p>
 * Best-effort throughout: an unavailable decision, an undeployed key or an empty result yields no warning rather than
 * an error, so the daily prepare is never blocked.
 * </p>
 */
@Service
public class ApplicationRulesService {

	static final String DECISION_KEY_QUESTIONS = "Decision_ansokanFragor";
	static final String DECISION_KEY_INCOME = "Decision_inkomstMotForegaende";
	static final String DECISION_KEY_CALCULATION = "Decision_ansokanMotBerakning";

	private static final String OUTPUT_VARNING = "varning";
	private static final String OUTPUT_VARNINGSKOD = "varningskod";
	private static final String OUTPUT_REGEL = "regel";

	/** The tables write "-" in the code/text columns of their no-warning rows; it is not a value to carry. */
	private static final String NO_VALUE = "-";

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationRulesService.class);

	private final ProcessService processService;

	ApplicationRulesService(final ProcessService processService) {
		this.processService = processService;
	}

	/**
	 * A rule verdict — whether to raise a warning, the stable warning code the table named ({@code varningskod}) and
	 * the verksamhet's warning text with its placeholders still in it ({@code regel}).
	 */
	public record RuleVerdict(boolean warning, String code, String rule) {

		/** The neutral verdict — nothing to flag. */
		public static RuleVerdict none() {
			return new RuleVerdict(false, null, null);
		}
	}

	/**
	 * Judge a single answer in the application.
	 *
	 * @param  municipalityId the municipality the errand belongs to
	 * @param  question       the question key (e.g. {@code BARN_BOENDE_OMFATTNING}, {@code BILAGOR})
	 * @param  answer         the answer in caremanagement's own vocabulary — an API constant
	 *                        ({@code FULL_TIME}, {@code SALARY}, {@code WORK}) or {@code "JA"}/{@code "NEJ"}
	 * @return                the verdict, best-effort
	 */
	public RuleVerdict question(final String municipalityId, final String question, final String answer) {
		final var variables = new HashMap<String, Object>();
		variables.put("fraga", orEmpty(question));
		variables.put("svar", orEmpty(answer));
		return evaluate(municipalityId, DECISION_KEY_QUESTIONS, variables);
	}

	/**
	 * Judge one income type in the previous normberäkning against the same type in the application. The table
	 * coalesces a missing sum to zero, so {@code null} sums are passed through as they are.
	 *
	 * @param  municipalityId the municipality the errand belongs to
	 * @param  incomeType     the financial assistance income type being compared (e.g. {@code SALARY})
	 * @param  onPrevious     whether the previous normberäkning carried the type at all
	 * @param  inApplication  whether the application declares an income of the type
	 * @param  previousAmount the previous normberäkning's summed amount for the type ({@code null} when absent)
	 * @param  applicationSum the application's summed amount for the type ({@code null} when absent)
	 * @return                the verdict, best-effort
	 */
	public RuleVerdict incomeAgainstPrevious(final String municipalityId, final String incomeType, final boolean onPrevious,
		final boolean inApplication, final BigDecimal previousAmount, final BigDecimal applicationSum) {

		final var variables = new HashMap<String, Object>();
		variables.put("inkomstslag", orEmpty(incomeType));
		variables.put("fannsForegaende", onPrevious);
		variables.put("finnsIAnsokan", inApplication);
		variables.put("summaForegaende", previousAmount);
		variables.put("summaAnsokan", applicationSum);
		return evaluate(municipalityId, DECISION_KEY_INCOME, variables);
	}

	/**
	 * Judge one application-versus-previous-normberäkning comparison. Whether the comparison should be made at all is
	 * the caller's gate (the verksamhet's "Nej = ingen jämförelse" rules); the table only decides the warning.
	 *
	 * @param  municipalityId the municipality the errand belongs to
	 * @param  comparison     the comparison key ({@code BARN_PERSONNUMMER}, {@code ANTAL_I_BOSTADEN}, {@code NORM})
	 * @param  sameAsPrevious whether the two sides are equal
	 * @return                the verdict, best-effort
	 */
	public RuleVerdict againstPreviousCalculation(final String municipalityId, final String comparison, final boolean sameAsPrevious) {
		final var variables = new HashMap<String, Object>();
		variables.put("jamforelse", orEmpty(comparison));
		variables.put("sammaSomForegaende", sameAsPrevious);
		return evaluate(municipalityId, DECISION_KEY_CALCULATION, variables);
	}

	private RuleVerdict evaluate(final String municipalityId, final String decisionKey, final Map<String, Object> variables) {
		try {
			final var rows = processService.evaluateDecision(municipalityId, decisionKey, variables);
			return firstRow(rows).map(ApplicationRulesService::toVerdict).orElseGet(RuleVerdict::none);
		} catch (final RuntimeException e) {
			LOG.warn("Återansökan rules ({}) unavailable — skipping the rule warning", decisionKey, e);
			return RuleVerdict.none();
		}
	}

	private static Optional<Map<String, Object>> firstRow(final List<Map<String, Object>> rows) {
		return ofNullable(rows).orElseGet(List::of).stream().findFirst();
	}

	private static RuleVerdict toVerdict(final Map<String, Object> row) {
		return new RuleVerdict(Boolean.TRUE.equals(row.get(OUTPUT_VARNING)), value(row.get(OUTPUT_VARNINGSKOD)), value(row.get(OUTPUT_REGEL)));
	}

	/** A table output as text, with the tables' own "no value" marker normalised away. */
	private static String value(final Object output) {
		return ofNullable(output).map(Object::toString).filter(text -> !NO_VALUE.equals(text)).orElse(null);
	}

	/** The value, or an empty string when it is {@code null}. */
	private static String orEmpty(final String value) {
		return ofNullable(value).orElse("");
	}
}
