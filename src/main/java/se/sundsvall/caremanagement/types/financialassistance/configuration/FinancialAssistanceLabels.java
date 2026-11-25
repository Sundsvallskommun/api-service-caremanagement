package se.sundsvall.caremanagement.types.financialassistance.configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import se.sundsvall.caremanagement.types.financialassistance.api.model.TypeOption;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.StringUtils.hasText;

/**
 * The Swedish labels for the module's string codes — the one place a code is turned into something a handläggare
 * reads.
 *
 * <p>
 * The codes themselves stay on the API (they are what the logic keys on); every surface that shows a code to a human
 * pairs it with the label from here: the calculation rows carry a {@code *DisplayName} beside the code, and the
 * warning texts are written with the label alone. Cost and income labels are not spelled out again — they are read
 * from {@link FinancialAssistanceTypes}, so the dropdown and the warning text can never drift apart.
 * </p>
 */
public final class FinancialAssistanceLabels {

	private FinancialAssistanceLabels() {}

	/** Household role → label. The roles are the {@code CalculationConstants.ROLE_*} codes. */
	private static final Map<String, String> ROLE_DISPLAY_NAME = Map.of(
		"APPLICANT", "Sökande",
		"CO_APPLICANT", "Medsökande",
		"CHILD", "Barn",
		"VISITATION_CHILD", "Umgängesbarn");

	/** Norm type → label (the {@code FinancialAssistanceData.normType} values). */
	private static final Map<String, String> NORM_TYPE_DISPLAY_NAME = Map.of(
		"NATIONAL_NORM", "Riksnorm",
		"OTHER_NORM", "Annan norm");

	/** Row provenance → label ({@code CalculationConstants.ORIGIN_*}). */
	private static final Map<String, String> ORIGIN_DISPLAY_NAME = Map.of(
		"SYSTEM", "Automatiskt",
		"APPLICATION", "Ansökan",
		"CASEWORKER", "Handläggare");

	private static final Map<String, TypeOption> COST_OPTION = byCode(FinancialAssistanceTypes.COST_TYPES);
	private static final Map<String, TypeOption> INCOME_OPTION = byCode(FinancialAssistanceTypes.INCOME_TYPES);

	/** The role's label, or {@code null} when the role is unknown or absent. */
	public static String roleDisplayName(final String role) {
		return lookup(ROLE_DISPLAY_NAME, role);
	}

	/** The norm type's label, or {@code null} when the norm type is unknown or absent. */
	public static String normTypeDisplayName(final String normType) {
		return lookup(NORM_TYPE_DISPLAY_NAME, normType);
	}

	/** The row provenance's label, or {@code null} when it is unknown or absent. */
	public static String originDisplayName(final String origin) {
		return lookup(ORIGIN_DISPLAY_NAME, origin);
	}

	/** A label lookup that answers {@code null} for an absent code — {@code Map.of} rejects a null key outright. */
	private static String lookup(final Map<String, String> labels, final String code) {
		if (!hasText(code)) {
			return null;
		}
		return labels.get(code);
	}

	/**
	 * The cost type's label — the Lifecare (handläggare) one, falling back to the Mina-sidor one and finally to the
	 * code itself, so a type the catalogue has not caught up with still renders as something rather than nothing.
	 */
	public static String costDisplayName(final String costType) {
		return displayName(COST_OPTION, costType);
	}

	/** The income type's label, with the same fallback chain as {@link #costDisplayName}. */
	public static String incomeDisplayName(final String incomeType) {
		return displayName(INCOME_OPTION, incomeType);
	}

	/**
	 * The income type as the applicant chose it on the application — the Mina-sidor label, falling back to the Lifecare
	 * one and finally to the code itself.
	 */
	public static String applicationIncomeDisplayName(final String incomeType) {
		if (!hasText(incomeType)) {
			return null;
		}
		return ofNullable(INCOME_OPTION.get(incomeType))
			.flatMap(option -> ofNullable(option.getExternalDisplayName()).filter(label -> hasText(label)).or(() -> firstLabel(option)))
			.orElse(incomeType);
	}

	private static String displayName(final Map<String, TypeOption> options, final String code) {
		if (!hasText(code)) {
			return null;
		}
		return ofNullable(options.get(code))
			.flatMap(FinancialAssistanceLabels::firstLabel)
			.orElse(code);
	}

	private static Optional<String> firstLabel(final TypeOption option) {
		return ofNullable(option.getInternalDisplayName()).filter(label -> hasText(label))
			.or(() -> ofNullable(option.getExternalDisplayName()).filter(label -> hasText(label)));
	}

	private static Map<String, TypeOption> byCode(final List<TypeOption> options) {
		return options.stream()
			.filter(option -> hasText(option.getCode()))
			.collect(toMap(TypeOption::getCode, identity(), (first, _) -> first));
	}
}
